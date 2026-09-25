package io.github.lxxz.pokebook.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.Optional;

/**
 * Uma mensagem como o cliente precisa vê-la.
 *
 * <p>{@code mine} em vez do UUID de quem mandou: é só o que a tela usa (de que lado desenhar
 * o balão), e numa conversa de duas pessoas diz tudo.
 */
public record ThreadMessage(boolean mine, String text, long time, Optional<String> photo) {
	public static final PacketCodec<RegistryByteBuf, ThreadMessage> CODEC = PacketCodec.tuple(
		PacketCodecs.BOOL, ThreadMessage::mine,
		PacketCodecs.STRING, ThreadMessage::text,
		PacketCodecs.VAR_LONG, ThreadMessage::time,
		PacketCodecs.optional(PacketCodecs.STRING), ThreadMessage::photo,
		ThreadMessage::new
	);
}
