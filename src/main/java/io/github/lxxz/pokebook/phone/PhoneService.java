package io.github.lxxz.pokebook.phone;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.config.ServerConfig;
import io.github.lxxz.pokebook.mission.Missions;
import io.github.lxxz.pokebook.network.ContactActionPayload;
import io.github.lxxz.pokebook.network.PhoneDataPayload;
import io.github.lxxz.pokebook.notify.NotificationKind;
import io.github.lxxz.pokebook.notify.Notifications;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * As regras do aparelho, do lado do servidor.
 *
 * <p>Todo pacote que o cliente manda para mexer nos próprios dados passa por aqui, e
 * <b>nenhum é aceito como veio</b>: contato só se salva com a pessoa online (é de lá que sai
 * o nome), nota e ponto passam por limite de tamanho e quantidade, ajuste passa por
 * saneamento. O cliente não é confiável, e um cliente modificado é o caso real que os
 * limites existem para conter.
 *
 * <p>Depois de toda mudança o cliente recebe os dados inteiros de volta — ver
 * {@link PhoneData}. A tela nunca aplica a mudança por conta própria esperando que o
 * servidor concorde: ela espera a resposta, e o que aparece é sempre o que foi gravado.
 */
public final class PhoneService {
	public static final AttachmentType<PhoneData> DATA = AttachmentRegistry.create(
		Identifier.of(Pokebook.MOD_ID, "phone"),
		builder -> builder
			.initializer(() -> PhoneData.EMPTY)
			.persistent(PhoneData.CODEC)
			// Sem isto morrer apagaria contatos, notas e pontos: o respawn cria uma
			// entidade nova, e o anexo só vai junto se pedido.
			.copyOnDeath()
	);

	private PhoneService() {
	}

	/** Força o carregamento da classe, e com ela o registro do anexo, no início do mod. */
	public static void register() {
	}

	public static PhoneData get(ServerPlayerEntity player) {
		return player.getAttachedOrCreate(DATA);
	}

	/** Grava e manda de volta. Toda mudança termina aqui. */
	public static void set(ServerPlayerEntity player, PhoneData data) {
		player.setAttached(DATA, data);
		sync(player);
	}

	public static void sync(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, new PhoneDataPayload(get(player)));
	}

	// ------------------------------------------------------------------ contatos

	public static void onContactAction(ServerPlayerEntity player, ContactActionPayload.Action action, UUID target) {
		switch (action) {
			case ADD -> addContact(player, target);
			case REMOVE -> removeContact(player, target);
			case TOGGLE_SHARE_LOCATION -> toggleShareLocation(player, target);
		}
	}

	private static void addContact(ServerPlayerEntity player, UUID target) {
		PhoneData data = get(player);
		if (target.equals(player.getUuid()) || data.hasContact(target)) {
			return;
		}
		if (data.contacts().size() >= PhoneData.MAX_CONTACTS) {
			player.sendMessage(Text.translatable("message.pokebook.contacts.full", PhoneData.MAX_CONTACTS), true);
			return;
		}
		// O nome sai do jogador conectado, nunca do pacote. É por isso que só se salva quem
		// está online: é o único momento em que o servidor sabe o nome de verdade.
		ServerPlayerEntity other = player.server.getPlayerManager().getPlayer(target);
		if (other == null) {
			return;
		}

		List<Contact> contacts = new ArrayList<>(data.contacts());
		contacts.add(new Contact(target, other.getGameProfile().getName(), false));
		set(player, data.withContacts(contacts));

		// Ser salvo não dá nada a quem salvou — o consentimento é sempre de quem recebe, ver
		// acceptsCallFrom. Avisar existe para o outro poder salvar de volta, se quiser.
		Notifications.send(other, NotificationKind.CONTACT,
			Text.translatable("notification.pokebook.contact.added.title"),
			Text.translatable("notification.pokebook.contact.added.body", player.getGameProfile().getName()));
	}

	private static void removeContact(ServerPlayerEntity player, UUID target) {
		PhoneData data = get(player);
		if (!data.hasContact(target)) {
			return;
		}
		List<Contact> contacts = new ArrayList<>(data.contacts());
		contacts.removeIf(c -> c.uuid().equals(target));
		set(player, data.withContacts(contacts));
	}

	private static void toggleShareLocation(ServerPlayerEntity player, UUID target) {
		PhoneData data = get(player);
		Optional<Contact> contact = data.contact(target);
		if (contact.isEmpty()) {
			return;
		}
		boolean share = !contact.get().shareLocation();
		if (share && !ServerConfig.features().locationSharing()) {
			player.sendMessage(Text.translatable("message.pokebook.feature_disabled"), true);
			return;
		}
		set(player, data.updateContact(target, c -> c.withShareLocation(share)));

		if (share) {
			ServerPlayerEntity other = player.server.getPlayerManager().getPlayer(target);
			if (other != null) {
				Notifications.send(other, NotificationKind.LOCATION,
					Text.translatable("notification.pokebook.location.shared.title"),
					Text.translatable("notification.pokebook.location.shared.body", player.getGameProfile().getName()));
			}
		}
	}

	/**
	 * Este jogador aceita uma ligação daquele?
	 *
	 * <p>É aqui que os contatos viram <b>consentimento</b>, e a regra é sempre do lado de
	 * quem <em>recebe</em>: salvar alguém não dá a você o direito de ligar para ele — dá a
	 * ele, se ele escolheu "só contatos", o direito de ligar para você.
	 */
	public static boolean acceptsCallFrom(ServerPlayerEntity callee, ServerPlayerEntity caller) {
		PhoneData data = get(callee);
		if (data.settings().doNotDisturb()) {
			return false;
		}
		return switch (data.settings().callPolicy()) {
			case EVERYONE -> true;
			case CONTACTS -> data.hasContact(caller.getUuid());
			case NOBODY -> false;
		};
	}

	/**
	 * Alguém entrou: manda os dados dele e atualiza o nome dele na lista de quem o salvou.
	 *
	 * <p>O nome guardado num contato é só o último conhecido. Trocar de apelido é raro, mas
	 * acontece, e o contato ficaria com o nome velho para sempre se ninguém o corrigisse.
	 * Corrigir ao entrar custa uma volta pela lista de conectados — quem está offline é
	 * corrigido quando ele mesmo entrar e o encontrar online.
	 */
	public static void onJoin(ServerPlayerEntity joined) {
		String name = joined.getGameProfile().getName();

		for (ServerPlayerEntity other : joined.server.getPlayerManager().getPlayerList()) {
			if (other == joined) {
				continue;
			}
			renameContact(other, joined.getUuid(), name);
			renameContact(joined, other.getUuid(), other.getGameProfile().getName());
		}
		sync(joined);
	}

	private static void renameContact(ServerPlayerEntity owner, UUID contact, String name) {
		PhoneData data = get(owner);
		data.contact(contact).ifPresent(c -> {
			if (!c.name().equals(name)) {
				set(owner, data.updateContact(contact, old -> old.withName(name)));
			}
		});
	}

	// ------------------------------------------------------------------ notas

	/**
	 * Grava ou apaga uma nota.
	 *
	 * <p>Nota vazia, depois de aparada, é apagada em vez de gravada: uma nota que o jogador
	 * esvaziou é uma nota que ele não quer mais, e guardá-la em branco deixaria um item sem
	 * nada para mostrar na lista.
	 */
	public static void onNoteAction(ServerPlayerEntity player, int index, Optional<String> text) {
		PhoneData data = get(player);
		List<String> notes = new ArrayList<>(data.notes());
		boolean existing = index >= 0 && index < notes.size();

		String clean = text.map(String::strip).orElse("");
		if (clean.length() > PhoneData.MAX_NOTE_LENGTH) {
			clean = clean.substring(0, PhoneData.MAX_NOTE_LENGTH);
		}

		if (clean.isEmpty()) {
			if (existing) {
				notes.remove(index);
				set(player, data.withNotes(notes));
			}
			return;
		}

		if (existing) {
			notes.set(index, clean);
		} else if (notes.size() < PhoneData.MAX_NOTES) {
			// Nota nova no topo: é a mais recente, e é a que o jogador quer ver primeiro.
			notes.add(0, clean);
		} else {
			player.sendMessage(Text.translatable("message.pokebook.notes.full", PhoneData.MAX_NOTES), true);
			return;
		}
		set(player, data.withNotes(notes));
	}

	// ------------------------------------------------------------------ pontos de interesse

	public static void onWaypointAction(ServerPlayerEntity player, int index, Optional<Waypoint> waypoint) {
		PhoneData data = get(player);
		List<Waypoint> waypoints = new ArrayList<>(data.waypoints());
		boolean existing = index >= 0 && index < waypoints.size();

		if (waypoint.isEmpty()) {
			if (existing) {
				waypoints.remove(index);
				set(player, data.withWaypoints(waypoints));
			}
			return;
		}

		Waypoint clean = waypoint.get().sanitized();
		if (existing) {
			waypoints.set(index, clean);
		} else if (waypoints.size() < PhoneData.MAX_WAYPOINTS) {
			waypoints.add(clean);
		} else {
			player.sendMessage(Text.translatable("message.pokebook.waypoints.full", PhoneData.MAX_WAYPOINTS), true);
			return;
		}
		set(player, data.withWaypoints(waypoints));
	}

	// ------------------------------------------------------------------ ajustes e missão

	public static void updateSettings(ServerPlayerEntity player, PhoneSettings settings) {
		PhoneData data = get(player);
		set(player, data.withSettings(settings.sanitized()));
	}

	/** Missão que não existe mais neste servidor é tratada como "nenhuma". */
	public static void trackMission(ServerPlayerEntity player, Optional<Identifier> missionId) {
		Optional<Identifier> valid = missionId.filter(id -> Missions.byId(id) != null);
		PhoneData data = get(player);
		if (!data.trackedMission().equals(valid)) {
			set(player, data.withTrackedMission(valid));
		}
	}
}
