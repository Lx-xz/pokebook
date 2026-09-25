package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Servidor → cliente: as suas conversas, da mais recente para a mais antiga.
 */
public record ConversationsPayload(List<ConversationSummary> conversations) implements CustomPayload {
	public static final CustomPayload.Id<ConversationsPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "conversations"));

	public static final PacketCodec<RegistryByteBuf, ConversationsPayload> CODEC =
		PacketCodec.tuple(
			ConversationSummary.CODEC.collect(PacketCodecs.toList()), ConversationsPayload::conversations,
			ConversationsPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
