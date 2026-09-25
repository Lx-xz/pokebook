package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.UUID;

/**
 * Servidor → cliente: as mensagens de uma conversa, da mais velha para a mais nova.
 */
public record ThreadPayload(UUID other, String name, List<ThreadMessage> messages) implements CustomPayload {
	public static final CustomPayload.Id<ThreadPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "thread"));

	public static final PacketCodec<RegistryByteBuf, ThreadPayload> CODEC =
		PacketCodec.tuple(
			Uuids.PACKET_CODEC, ThreadPayload::other,
			PacketCodecs.STRING, ThreadPayload::name,
			ThreadMessage.CODEC.collect(PacketCodecs.toList()), ThreadPayload::messages,
			ThreadPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
