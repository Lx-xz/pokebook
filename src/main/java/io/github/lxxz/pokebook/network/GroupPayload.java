package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Servidor → cliente: o seu grupo. Lista vazia é "sem grupo".
 *
 * <p>Vai como resposta ao pedido e também sozinho, quando o grupo muda — alguém entrou,
 * saiu, ou o grupo acabou.
 */
public record GroupPayload(List<GroupMember> members) implements CustomPayload {
	public static final CustomPayload.Id<GroupPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "group"));

	public static final PacketCodec<RegistryByteBuf, GroupPayload> CODEC =
		PacketCodec.tuple(
			GroupMember.CODEC.collect(PacketCodecs.toList()), GroupPayload::members,
			GroupPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
