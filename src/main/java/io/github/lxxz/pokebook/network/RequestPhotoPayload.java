package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: quero ver esta foto compartilhada.
 *
 * <p>O servidor só entrega a quem participa de uma conversa em que ela foi mandada.
 */
public record RequestPhotoPayload(String photoId) implements CustomPayload {
	public static final CustomPayload.Id<RequestPhotoPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "request_photo"));

	public static final PacketCodec<RegistryByteBuf, RequestPhotoPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.STRING, RequestPhotoPayload::photoId,
			RequestPhotoPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
