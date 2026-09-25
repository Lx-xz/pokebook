package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Cliente → servidor: acompanhe esta missão no HUD, ou nenhuma.
 *
 * <p>Guardado no servidor, e não só no cliente, porque é por mundo: os ids de missão de um
 * servidor não existem no outro. E sobrevive a sair e voltar, que é o que se espera de
 * "estou acompanhando esta".
 */
public record TrackMissionPayload(Optional<Identifier> missionId) implements CustomPayload {
	public static final CustomPayload.Id<TrackMissionPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "track_mission"));

	public static final PacketCodec<RegistryByteBuf, TrackMissionPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.optional(Identifier.PACKET_CODEC), TrackMissionPayload::missionId,
			TrackMissionPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
