package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Cliente → servidor: mande este texto para esta pessoa.
 *
 * <p>Consentimento, tamanho e frequência são conferidos no servidor — ver {@code MessageService}.
 */
public record SendMessagePayload(UUID to, String text) implements CustomPayload {
	public static final CustomPayload.Id<SendMessagePayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "send_message"));

	public static final PacketCodec<RegistryByteBuf, SendMessagePayload> CODEC =
		PacketCodec.tuple(
			Uuids.PACKET_CODEC, SendMessagePayload::to,
			PacketCodecs.string(MessageLimits.MAX_TEXT), SendMessagePayload::text,
			SendMessagePayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
