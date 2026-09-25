package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente → servidor: saio do meu grupo. */
public record LeaveGroupPayload() implements CustomPayload {
	public static final CustomPayload.Id<LeaveGroupPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "leave_group"));

	public static final PacketCodec<RegistryByteBuf, LeaveGroupPayload> CODEC =
		PacketCodec.unit(new LeaveGroupPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
