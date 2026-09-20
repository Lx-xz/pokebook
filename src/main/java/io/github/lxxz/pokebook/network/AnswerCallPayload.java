package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: atendi.
 *
 * <p>Não carrega nada. Qual ligação é não precisa viajar — o servidor já sabe quem mandou
 * o pacote, e um jogador tem no máximo uma ligação. Mandar o identificador dela seria dar
 * ao cliente uma escolha que ele não tem.
 */
public record AnswerCallPayload() implements CustomPayload {
	public static final CustomPayload.Id<AnswerCallPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "answer_call"));

	public static final PacketCodec<RegistryByteBuf, AnswerCallPayload> CODEC =
		PacketCodec.unit(new AnswerCallPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
