package io.github.lxxz.pokebook.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

import java.util.UUID;

/** Um membro do grupo, como a tela precisa vê-lo. */
public record GroupMember(UUID uuid, String name, boolean online) {
	public static final PacketCodec<RegistryByteBuf, GroupMember> CODEC = PacketCodec.tuple(
		Uuids.PACKET_CODEC, GroupMember::uuid,
		PacketCodecs.STRING, GroupMember::name,
		PacketCodecs.BOOL, GroupMember::online,
		GroupMember::new
	);
}
