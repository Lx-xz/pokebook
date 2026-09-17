package io.github.lxxz.pokebook.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

/**
 * Uma missão como o cliente precisa vê-la.
 *
 * <p>Vai só o necessário para desenhar a linha. O nome não viaja: o cliente monta a
 * chave de tradução a partir do id, e assim cada jogador lê no próprio idioma em vez de
 * no idioma do servidor.
 */
public record MissionEntry(Identifier id, int count, int required, ItemStack reward, boolean claimed) {
	public static final PacketCodec<RegistryByteBuf, MissionEntry> CODEC = PacketCodec.tuple(
		Identifier.PACKET_CODEC, MissionEntry::id,
		PacketCodecs.VAR_INT, MissionEntry::count,
		PacketCodecs.VAR_INT, MissionEntry::required,
		ItemStack.PACKET_CODEC, MissionEntry::reward,
		PacketCodecs.BOOL, MissionEntry::claimed,
		MissionEntry::new
	);

	public boolean complete() {
		return count >= required;
	}

	public boolean claimable() {
		return complete() && !claimed;
	}
}
