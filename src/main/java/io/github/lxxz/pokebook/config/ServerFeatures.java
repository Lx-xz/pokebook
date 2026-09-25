package io.github.lxxz.pokebook.config;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * O que o dono do servidor deixou ligado.
 *
 * <p>São as funcionalidades que variam muito de servidor para servidor: num servidor de
 * construção entre amigos, ver onde o outro está é conveniência; num de PvP é arma. O
 * padrão é tudo ligado, porque o caso comum é o primeiro.
 *
 * <p>Viaja para o cliente só para ele <b>esconder o ícone</b> do que está desligado. A
 * regra mesma é conferida no servidor onde o servidor tem como — compartilhar
 * localização e mandá-la no chat passam por ele. Radar e fotos, não: um vê entidades que o
 * jogo já mandou ao cliente, o outro é uma captura de tela local. Para esses dois,
 * desligar é tirar o botão, e um cliente modificado continua podendo fazer o que o
 * vanilla já deixa (ver entidades próximas, tirar print).
 */
public record ServerFeatures(boolean locationSharing, boolean chatLocation, boolean radar, boolean photos) {
	public static final ServerFeatures ALL = new ServerFeatures(true, true, true, true);

	public static final PacketCodec<RegistryByteBuf, ServerFeatures> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.BOOL, ServerFeatures::locationSharing,
		PacketCodecs.BOOL, ServerFeatures::chatLocation,
		PacketCodecs.BOOL, ServerFeatures::radar,
		PacketCodecs.BOOL, ServerFeatures::photos,
		ServerFeatures::new
	);
}
