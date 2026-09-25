package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.GlobalPos;

/**
 * Servidor → cliente: aponte a seta para aqui.
 *
 * <p>É a resposta do comando {@code /pokebook rastrear}, que é o que o texto clicável de
 * uma localização mandada no chat executa. O servidor não guarda nada: a seta é estado do
 * cliente, e este pacote só a liga.
 */
public record TrackTargetPayload(GlobalPos pos, String label) implements CustomPayload {
	public static final CustomPayload.Id<TrackTargetPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "track_target"));

	public static final PacketCodec<RegistryByteBuf, TrackTargetPayload> CODEC =
		PacketCodec.tuple(
			GlobalPos.PACKET_CODEC, TrackTargetPayload::pos,
			PacketCodecs.STRING, TrackTargetPayload::label,
			TrackTargetPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
