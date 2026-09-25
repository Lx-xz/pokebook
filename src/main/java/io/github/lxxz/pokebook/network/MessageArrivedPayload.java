package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Servidor → cliente: chegou mensagem na conversa com esta pessoa.
 *
 * <p>Não traz a mensagem: a tela aberta pede a conversa de novo, e a fechada não precisa de
 * nada — o aviso vai pela central de notificações. Assim há um jeito só de o cliente saber
 * o conteúdo de uma conversa, que é pedindo.
 */
public record MessageArrivedPayload(UUID other) implements CustomPayload {
	public static final CustomPayload.Id<MessageArrivedPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "message_arrived"));

	public static final PacketCodec<RegistryByteBuf, MessageArrivedPayload> CODEC =
		PacketCodec.tuple(
			Uuids.PACKET_CODEC, MessageArrivedPayload::other,
			MessageArrivedPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
