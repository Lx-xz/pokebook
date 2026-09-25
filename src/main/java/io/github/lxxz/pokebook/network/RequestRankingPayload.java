package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: quero o ranking.
 *
 * <p>Sob demanda. O ranking inclui quem está offline e cresce com a idade do servidor, então
 * é o último dado que deveria ir junto de toda abertura do pokébook.
 */
public record RequestRankingPayload() implements CustomPayload {
	public static final CustomPayload.Id<RequestRankingPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "request_ranking"));

	public static final PacketCodec<RegistryByteBuf, RequestRankingPayload> CODEC =
		PacketCodec.unit(new RequestRankingPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
