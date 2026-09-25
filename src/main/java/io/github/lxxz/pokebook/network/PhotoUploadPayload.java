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
 * Cliente → servidor: um pedaço de uma foto sendo mandada para alguém.
 *
 * <p>Em pedaços porque o pacote do cliente para o servidor tem limite de 32 767 bytes, e uma
 * foto reduzida tem por volta de 100 KB. {@code upload} identifica o envio, para dois envios
 * seguidos não misturarem pedaços; {@code index} e {@code total} dizem onde este cai.
 */
public record PhotoUploadPayload(UUID to, int upload, int index, int total, byte[] data) implements CustomPayload {
	public static final CustomPayload.Id<PhotoUploadPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "photo_upload"));

	public static final PacketCodec<RegistryByteBuf, PhotoUploadPayload> CODEC =
		PacketCodec.tuple(
			Uuids.PACKET_CODEC, PhotoUploadPayload::to,
			PacketCodecs.VAR_INT, PhotoUploadPayload::upload,
			PacketCodecs.VAR_INT, PhotoUploadPayload::index,
			PacketCodecs.VAR_INT, PhotoUploadPayload::total,
			PacketCodecs.byteArray(MessageLimits.PHOTO_CHUNK), PhotoUploadPayload::data,
			PhotoUploadPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
