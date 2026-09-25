package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Cliente → servidor: desafie este jogador para uma batalha.
 *
 * <p>Só o {@link UUID}; consentimento, disponibilidade e se há Cobblemon são conferidos no
 * servidor — ver {@code BattleChallenges}.
 */
public record ChallengePayload(UUID target) implements CustomPayload {
	public static final CustomPayload.Id<ChallengePayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "challenge"));

	public static final PacketCodec<RegistryByteBuf, ChallengePayload> CODEC =
		PacketCodec.tuple(
			Uuids.PACKET_CODEC, ChallengePayload::target,
			ChallengePayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
