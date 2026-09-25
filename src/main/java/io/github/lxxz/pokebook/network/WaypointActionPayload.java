package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.phone.Waypoint;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Cliente → servidor: grave ou apague um ponto de interesse.
 *
 * <p>Mesma forma das notas: ponto presente é gravar, ausente é apagar, índice negativo é
 * ponto novo. O ponto vem com coordenadas escolhidas pelo cliente, e não há o que conferir
 * nelas — são as coordenadas <em>dele</em>, que ele poderia digitar no chat de qualquer
 * jeito. O que o servidor confere é o nome e o ícone, ver {@link Waypoint#sanitized()}.
 */
public record WaypointActionPayload(int index, Optional<Waypoint> waypoint) implements CustomPayload {
	public static final CustomPayload.Id<WaypointActionPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "waypoint_action"));

	public static final PacketCodec<RegistryByteBuf, WaypointActionPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.VAR_INT, WaypointActionPayload::index,
			PacketCodecs.optional(PacketCodecs.codec(Waypoint.CODEC)), WaypointActionPayload::waypoint,
			WaypointActionPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
