package io.github.lxxz.pokebook.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

import java.util.UUID;

/** Uma linha da lista de conversas: com quem, a última mensagem, quando, e quantas novas. */
public record ConversationSummary(UUID other, String name, String preview, long time, int unread) {
	public static final PacketCodec<RegistryByteBuf, ConversationSummary> CODEC = PacketCodec.tuple(
		Uuids.PACKET_CODEC, ConversationSummary::other,
		PacketCodecs.STRING, ConversationSummary::name,
		PacketCodecs.STRING, ConversationSummary::preview,
		PacketCodecs.VAR_LONG, ConversationSummary::time,
		PacketCodecs.VAR_INT, ConversationSummary::unread,
		ConversationSummary::new
	);
}
