package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.hud.Navigation;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.ContactActionPayload;
import io.github.lxxz.pokebook.phone.Contact;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Contatos: quem você salvou, e quem está online para salvar.
 *
 * <p>Contato é <b>camada de consentimento</b>, e a regra é sempre de quem recebe: salvar
 * alguém não te dá nada sobre ele. Dá a ele, se ele escolher "só contatos", o direito de te
 * ligar; e te deixa escolher, contato a contato, quem vê onde você está (o ◎).
 *
 * <p>Salva-se só quem está online, porque é de lá que o servidor tira o nome — um nome vindo
 * do cliente poderia dizer qualquer coisa. Por isso a tela tem duas partes: os salvos, e
 * embaixo quem está conectado e ainda não foi salvo.
 *
 * <p>Tocar num contato que compartilha a localização com você liga a seta para segui-lo.
 */
public class ContactsScreen extends ScrollListScreen<ContactsScreen.Row> {
	private static final int FACE = 16;
	/** Largura de cada botãozinho no fim da linha. */
	private static final int ACTION = 12;

	/** Quanto tempo o ✕ fica armado esperando o segundo clique, em milissegundos. */
	private static final long CONFIRM_MS = 3000;

	public sealed interface Row permits Header, Person {
	}

	/** Um título de seção. Não responde a clique. */
	public record Header(Text text) implements Row {
	}

	/**
	 * Uma pessoa.
	 *
	 * @param saved        está nos seus contatos
	 * @param iShare       você deixa este contato ver onde você está
	 * @param sharesWithMe ele deixa você ver onde ele está — e aí dá para segui-lo
	 */
	public record Person(UUID uuid, String name, boolean saved, boolean online,
			boolean iShare, boolean sharesWithMe) implements Row {
	}

	/** Contato com o ✕ armado, esperando confirmação, e desde quando. */
	private UUID armedRemoval;
	private long armedAt;

	public ContactsScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.contacts"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	@Override
	protected Text emptyText() {
		return Text.translatable("screen.pokebook.contacts.empty");
	}

	@Override
	protected List<Row> rows() {
		List<Row> rows = new ArrayList<>();
		Set<UUID> saved = new HashSet<>();

		List<Contact> contacts = new ArrayList<>(ClientPhone.data().contacts());
		// Quem está online primeiro — é com quem dá para falar agora —, e dentro de cada
		// grupo por nome, para a lista não dançar entre um quadro e outro.
		contacts.sort(Comparator.comparing((Contact c) -> !PlayerFaces.isOnline(c.uuid()))
			.thenComparing(Contact::name, String.CASE_INSENSITIVE_ORDER));

		if (!contacts.isEmpty()) {
			rows.add(new Header(Text.translatable("screen.pokebook.contacts.saved")));
			for (Contact contact : contacts) {
				saved.add(contact.uuid());
				rows.add(new Person(contact.uuid(), contact.name(), true, PlayerFaces.isOnline(contact.uuid()),
					contact.shareLocation(), ClientPhone.sharedLocationOf(contact.uuid()).isPresent()));
			}
		}

		List<Person> others = new ArrayList<>();
		ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
		UUID self = client != null && client.player != null ? client.player.getUuid() : null;
		if (handler != null) {
			for (PlayerListEntry entry : handler.getPlayerList()) {
				UUID uuid = entry.getProfile().getId();
				if (!uuid.equals(self) && !saved.contains(uuid)) {
					others.add(new Person(uuid, entry.getProfile().getName(), false, true, false, false));
				}
			}
		}
		if (!others.isEmpty()) {
			others.sort(Comparator.comparing(Person::name, String.CASE_INSENSITIVE_ORDER));
			rows.add(new Header(Text.translatable("screen.pokebook.contacts.online")));
			rows.addAll(others);
		}
		return rows;
	}

	private boolean armed(UUID uuid) {
		return uuid.equals(armedRemoval) && System.currentTimeMillis() - armedAt < CONFIRM_MS;
	}

	private boolean sharingAllowed() {
		return ClientPhone.features().locationSharing();
	}

	// ------------------------------------------------------------------ desenho

	@Override
	protected void renderRow(DrawContext context, Row row, int x, int y, int width, boolean hovered) {
		int textY = y + (ROW_HEIGHT - textRenderer.fontHeight) / 2;

		if (row instanceof Header header) {
			context.drawText(textRenderer, header.text(), x, textY + 2, COLOR_MUTED, false);
			return;
		}
		Person person = (Person) row;

		PlayerFaces.draw(context, PlayerFaces.skin(person.uuid()), x, y + (ROW_HEIGHT - FACE) / 2, FACE, person.online());

		// Os botõezinhos, da direita para a esquerda.
		int right = x + width;
		if (person.saved()) {
			right -= ACTION;
			boolean armed = armed(person.uuid());
			glyph(context, "✕", right, textY, armed ? 0xFFB02E26 : COLOR_MUTED);
			if (hovered && isOver(right, y, ACTION, ROW_HEIGHT)) {
				tooltip(Text.translatable(armed
					? "screen.pokebook.contacts.remove_confirm"
					: "screen.pokebook.contacts.remove"));
			}

			if (sharingAllowed()) {
				right -= ACTION;
				glyph(context, "◎", right, textY, person.iShare() ? COLOR_ACCENT : COLOR_MUTED);
				if (hovered && isOver(right, y, ACTION, ROW_HEIGHT)) {
					tooltip(Text.translatable(person.iShare()
						? "screen.pokebook.contacts.share_on"
						: "screen.pokebook.contacts.share_off"));
				}
			}
		} else {
			right -= ACTION;
			glyph(context, "+", right, textY, hovered ? COLOR_ACCENT : COLOR_MUTED);
			if (hovered) {
				tooltip(Text.translatable("screen.pokebook.contacts.add"));
			}
		}

		int nameX = x + FACE + 4;
		int nameWidth = right - 2 - nameX;
		int color = !person.online() ? COLOR_MUTED : person.sharesWithMe() ? COLOR_DONE : COLOR_TEXT;
		context.drawText(textRenderer, textRenderer.trimToWidth(person.name(), nameWidth), nameX, textY, color, false);

		if (person.saved() && person.sharesWithMe() && hovered && isOver(x, y, right - x, ROW_HEIGHT)) {
			tooltip(Text.translatable("screen.pokebook.contacts.follow"));
		}
	}

	private void glyph(DrawContext context, String glyph, int x, int y, int color) {
		context.drawText(textRenderer, glyph, x + (ACTION - textRenderer.getWidth(glyph)) / 2, y, color, false);
	}

	// ------------------------------------------------------------------ interação

	@Override
	protected void onRowClicked(Row row, int index, double localX) {
		if (!(row instanceof Person person)) {
			return;
		}
		int width = contentWidth();

		if (!person.saved()) {
			ClientPlayNetworking.send(new ContactActionPayload(ContactActionPayload.Action.ADD, person.uuid()));
			return;
		}

		if (localX >= width - ACTION) {
			if (armed(person.uuid())) {
				armedRemoval = null;
				ClientPlayNetworking.send(new ContactActionPayload(ContactActionPayload.Action.REMOVE, person.uuid()));
			} else {
				armedRemoval = person.uuid();
				armedAt = System.currentTimeMillis();
			}
			return;
		}

		if (sharingAllowed() && localX >= width - ACTION * 2) {
			ClientPlayNetworking.send(new ContactActionPayload(
				ContactActionPayload.Action.TOGGLE_SHARE_LOCATION, person.uuid()));
			return;
		}

		if (person.sharesWithMe()) {
			Navigation.navigateTo(new Navigation.FollowPlayer(person.uuid(), person.name()));
			close();
		}
	}
}
