package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Cliente → servidor: convide este jogador para o meu grupo.
 *
 * <p>Só o {@link UUID}; consentimento, lotação e se ele já tem grupo são conferidos no
 * servidor — ver {@code Groups}.
 */
public record GroupInvitePayload(UUID target) implements CustomPayload {
	public static final CustomPayload.Id<GroupInvitePayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "group_invite"));

	public static final PacketCodec<RegistryByteBuf, GroupInvitePayload> CODEC =
		PacketCodec.tuple(
			Uuids.PACKET_CODEC, GroupInvitePayload::target,
			GroupInvitePayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
