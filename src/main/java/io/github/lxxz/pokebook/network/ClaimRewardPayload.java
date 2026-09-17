package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: resgatar a recompensa de uma missão.
 *
 * <p>Carrega só o id da missão. Tudo mais — se está completa, se já foi resgatada, se
 * cabe no inventário — é decidido no servidor. O cliente pede; quem julga é o servidor.
 */
public record ClaimRewardPayload(Identifier missionId) implements CustomPayload {
	public static final CustomPayload.Id<ClaimRewardPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "claim"));

	public static final PacketCodec<RegistryByteBuf, ClaimRewardPayload> CODEC =
		PacketCodec.tuple(
			Identifier.PACKET_CODEC, ClaimRewardPayload::missionId,
			ClaimRewardPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
