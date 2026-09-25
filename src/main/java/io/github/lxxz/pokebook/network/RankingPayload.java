package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/** Servidor → cliente: o ranking de missões, já ordenado. */
public record RankingPayload(List<RankingEntry> entries) implements CustomPayload {
	public static final CustomPayload.Id<RankingPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "ranking"));

	public static final PacketCodec<RegistryByteBuf, RankingPayload> CODEC =
		PacketCodec.tuple(
			RankingEntry.CODEC.collect(PacketCodecs.toList()), RankingPayload::entries,
			RankingPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
