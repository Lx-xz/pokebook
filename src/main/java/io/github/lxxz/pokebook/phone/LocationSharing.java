package io.github.lxxz.pokebook.phone;

import io.github.lxxz.pokebook.config.ServerConfig;
import io.github.lxxz.pokebook.network.SharedLocation;
import io.github.lxxz.pokebook.network.SharedLocationsPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.GlobalPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manda a cada jogador onde estão os contatos que escolheram mostrar a localização para
 * ele.
 *
 * <p><b>Quem decide é quem é visto.</b> A marca "compartilhar" mora no contato do lado de
 * quem compartilha — ver {@link Contact}. Salvar alguém não dá o direito de ver onde ele
 * está; ele é que precisa te salvar e ligar a marca. E o "não perturbe" suspende tudo, que
 * é o que "modo avião" quer dizer.
 *
 * <p>Uma vez por segundo, e não a cada tick: a seta não precisa de mais que isso para
 * andar junto, e cada envio é um pacote por jogador que recebe.
 *
 * <p>⚠️ A conta é todos contra todos — para cada conectado, olha cada outro conectado. Num
 * servidor de dezenas de jogadores isso é nada; se um dia forem centenas, o certo é manter
 * um índice reverso de "quem compartilha com quem" em vez de recalcular.
 *
 * <p>⚠️ <b>Em servidor de PvP isto é arma.</b> O dono pode desligar em
 * {@code config/pokebook-server.json} ({@code location_sharing}), e aí nada é mandado —
 * nem para quem já tinha a marca ligada.
 */
public final class LocationSharing {
	private static final int INTERVAL_TICKS = 20;

	/**
	 * Quem recebeu uma lista não vazia no último envio.
	 *
	 * <p>Existe para mandar a lista vazia <b>uma vez</b> quando o último contato para de
	 * compartilhar. Sem isso a seta de quem parou ficaria apontando para a última posição
	 * conhecida, e mandar lista vazia a todo mundo todo segundo seria desperdício.
	 */
	private static final Set<UUID> RECEIVING = new HashSet<>();

	private LocationSharing() {
	}

	public static void tick(MinecraftServer server) {
		if (server.getTicks() % INTERVAL_TICKS != 0) {
			return;
		}
		boolean enabled = ServerConfig.features().locationSharing();
		List<ServerPlayerEntity> online = server.getPlayerManager().getPlayerList();

		for (ServerPlayerEntity viewer : online) {
			List<SharedLocation> visible = enabled ? visibleTo(viewer, online) : List.of();
			boolean wasReceiving = RECEIVING.contains(viewer.getUuid());

			if (visible.isEmpty()) {
				if (wasReceiving) {
					RECEIVING.remove(viewer.getUuid());
					ServerPlayNetworking.send(viewer, new SharedLocationsPayload(List.of()));
				}
				continue;
			}
			RECEIVING.add(viewer.getUuid());
			ServerPlayNetworking.send(viewer, new SharedLocationsPayload(visible));
		}
	}

	private static List<SharedLocation> visibleTo(ServerPlayerEntity viewer, List<ServerPlayerEntity> online) {
		List<SharedLocation> visible = new ArrayList<>();
		for (ServerPlayerEntity other : online) {
			if (other == viewer) {
				continue;
			}
			PhoneData data = PhoneService.get(other);
			if (data.settings().doNotDisturb()) {
				continue;
			}
			boolean shares = data.contact(viewer.getUuid()).map(Contact::shareLocation).orElse(false);
			if (shares) {
				visible.add(new SharedLocation(other.getUuid(), other.getGameProfile().getName(),
					GlobalPos.create(other.getServerWorld().getRegistryKey(), other.getBlockPos())));
			}
		}
		return visible;
	}

	/** Quem saiu não recebe mais nada — e, ao voltar, começa do zero. */
	public static void disconnect(ServerPlayerEntity player) {
		RECEIVING.remove(player.getUuid());
	}
}
