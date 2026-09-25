package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Servidor → cliente: uma foto compartilhada, inteira.
 *
 * <p>De uma vez só: o limite no sentido servidor → cliente é de 1 MB, e a foto guardada tem
 * no máximo {@link MessageLimits#MAX_PHOTO_BYTES}.
 */
public record PhotoDataPayload(String photoId, byte[] png) implements CustomPayload {
	public static final CustomPayload.Id<PhotoDataPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "photo_data"));

	public static final PacketCodec<RegistryByteBuf, PhotoDataPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.STRING, PhotoDataPayload::photoId,
			PacketCodecs.byteArray(MessageLimits.MAX_PHOTO_BYTES), PhotoDataPayload::png,
			PhotoDataPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
