package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Cliente → servidor: quero as mensagens da conversa com esta pessoa.
 *
 * <p>Pedir a conversa é também lê-la: o servidor marca como lida ao responder.
 */
public record RequestThreadPayload(UUID other) implements CustomPayload {
	public static final CustomPayload.Id<RequestThreadPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "request_thread"));

	public static final PacketCodec<RegistryByteBuf, RequestThreadPayload> CODEC =
		PacketCodec.tuple(
			Uuids.PACKET_CODEC, RequestThreadPayload::other,
			RequestThreadPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
