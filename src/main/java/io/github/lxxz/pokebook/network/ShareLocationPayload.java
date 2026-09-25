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
 * Cliente → servidor: mande esta localização no chat.
 *
 * <p>Sem ponto é "onde eu estou agora", e aí a posição é a que o <b>servidor</b> sabe, não
 * a que o cliente diria. Com ponto é um ponto de interesse dele, com as coordenadas dele.
 */
public record ShareLocationPayload(Optional<Waypoint> waypoint) implements CustomPayload {
	public static final CustomPayload.Id<ShareLocationPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "share_location"));

	public static final PacketCodec<RegistryByteBuf, ShareLocationPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.optional(PacketCodecs.codec(Waypoint.CODEC)), ShareLocationPayload::waypoint,
			ShareLocationPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
