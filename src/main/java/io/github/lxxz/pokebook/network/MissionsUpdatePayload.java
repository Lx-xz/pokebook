package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Servidor → cliente: a lista de missões mudou; redesenhe.
 *
 * <p>Existe para que o resgate atualize a tela sem reabri-la. Reenviar o pacote de
 * abertura funcionaria, mas jogaria o jogador de volta ao menu inicial no meio da ação.
 */
public record MissionsUpdatePayload(List<MissionEntry> missions) implements CustomPayload {
	public static final CustomPayload.Id<MissionsUpdatePayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "missions_update"));

	public static final PacketCodec<RegistryByteBuf, MissionsUpdatePayload> CODEC =
		PacketCodec.tuple(
			MissionEntry.CODEC.collect(PacketCodecs.toList()), MissionsUpdatePayload::missions,
			MissionsUpdatePayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
