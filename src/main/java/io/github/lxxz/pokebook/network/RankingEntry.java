package io.github.lxxz.pokebook.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Uma linha do ranking: quem, quantas concluiu, e se está conectado agora.
 *
 * <p>Parente da {@link SocialEntry}, mas outra pergunta: a aba social diz quem está na
 * frente <em>agora, entre os presentes</em>; o ranking diz quem está na frente de todos
 * os tempos, inclusive quem não entra há semanas.
 */
public record RankingEntry(String name, int completed, boolean online) {
	public static final PacketCodec<RegistryByteBuf, RankingEntry> CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, RankingEntry::name,
		PacketCodecs.VAR_INT, RankingEntry::completed,
		PacketCodecs.BOOL, RankingEntry::online,
		RankingEntry::new
	);
}
