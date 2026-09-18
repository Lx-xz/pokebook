package io.github.lxxz.pokebook.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

/**
 * Uma missão como o cliente precisa vê-la.
 *
 * <p>O título viaja como {@link Text}, e não como string pronta: uma missão do mod manda
 * um texto <em>traduzível</em>, que o cliente resolve no próprio idioma, e uma missão de
 * datapack com {@code "title"} manda um texto literal. Os dois casos cabem no mesmo campo
 * sem que o cliente precise saber de qual se trata.
 */
public record MissionEntry(Identifier id, Text title, int count, int required, ItemStack reward, boolean claimed) {
	public static final PacketCodec<RegistryByteBuf, MissionEntry> CODEC = PacketCodec.tuple(
		Identifier.PACKET_CODEC, MissionEntry::id,
		TextCodecs.REGISTRY_PACKET_CODEC, MissionEntry::title,
		PacketCodecs.VAR_INT, MissionEntry::count,
		PacketCodecs.VAR_INT, MissionEntry::required,
		ItemStack.OPTIONAL_PACKET_CODEC, MissionEntry::reward,
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
