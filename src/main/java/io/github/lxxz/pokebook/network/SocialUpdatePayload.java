package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/** Servidor → cliente: o quadro de todos os jogadores conectados. */
public record SocialUpdatePayload(List<SocialEntry> players) implements CustomPayload {
	public static final CustomPayload.Id<SocialUpdatePayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "social"));

	public static final PacketCodec<RegistryByteBuf, SocialUpdatePayload> CODEC =
		PacketCodec.tuple(
			SocialEntry.CODEC.collect(PacketCodecs.toList()), SocialUpdatePayload::players,
			SocialUpdatePayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
