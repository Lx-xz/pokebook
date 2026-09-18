package io.github.lxxz.pokebook.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Como um outro jogador aparece na aba social.
 *
 * <p>Vai um resumo, não a lista de missões dele. Ver "quantas de quantas" responde a
 * pergunta que a aba existe para responder — quem está na frente — sem multiplicar o
 * tamanho do pacote pelo número de jogadores vezes o número de missões.
 *
 * <p>O nome viaja como texto simples porque é isso que ele é: um apelido, não algo
 * traduzível.
 */
public record SocialEntry(String name, int completed, int claimed, int total) {
	public static final PacketCodec<RegistryByteBuf, SocialEntry> CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, SocialEntry::name,
		PacketCodecs.VAR_INT, SocialEntry::completed,
		PacketCodecs.VAR_INT, SocialEntry::claimed,
		PacketCodecs.VAR_INT, SocialEntry::total,
		SocialEntry::new
	);
}
