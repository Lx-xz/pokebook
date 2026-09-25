package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: quero a lista das minhas conversas.
 *
 * <p>Sob demanda: só quem abre o app de mensagens paga pela resposta.
 */
public record RequestConversationsPayload() implements CustomPayload {
	public static final CustomPayload.Id<RequestConversationsPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "request_conversations"));

	public static final PacketCodec<RegistryByteBuf, RequestConversationsPayload> CODEC =
		PacketCodec.unit(new RequestConversationsPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
