package io.github.lxxz.pokebook.config;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * O que está ligado neste servidor.
 *
 * <p>São as funcionalidades que variam muito de servidor para servidor: num servidor de
 * construção entre amigos, ver onde o outro está é conveniência; num de PvP é arma. O
 * padrão é tudo ligado, porque o caso comum é o primeiro.
 *
 * <p>Viaja para o cliente só para ele <b>esconder o ícone</b> do que está desligado. A
 * regra mesma é conferida no servidor onde o servidor tem como — compartilhar
 * localização, mandar no chat, mensagens, fotos compartilhadas e lanterna passam por ele.
 * Radar e fotos locais, não: um vê entidades que o jogo já mandou ao cliente, o outro é
 * uma captura de tela local.
 *
 * <p>{@code cobblemon} não é configuração: é se o <b>servidor</b> tem o Cobblemon. É o que
 * o cliente consulta para oferecer PC remoto e desafio de batalha — perguntar ao próprio
 * cliente não serve, porque quem precisa ter o mod para isso funcionar é o servidor.
 */
public record ServerFeatures(
	boolean locationSharing,
	boolean chatLocation,
	boolean radar,
	boolean photos,
	boolean messages,
	boolean photoSharing,
	boolean flashlight,
	boolean cobblemon
) {
	public static final ServerFeatures ALL = new ServerFeatures(true, true, true, true, true, true, true, false);

	/**
	 * Um bit por campo, num inteiro só.
	 *
	 * <p>Não é economia de bytes: o {@code PacketCodec.tuple} da 1.21.1 aceita no máximo seis
	 * campos, e aqui são oito. Aninhar tuplas seria mais código para dizer a mesma coisa.
	 * Bit desconhecido de uma versão futura é ignorado; bit ausente de uma antiga vale
	 * "desligado", que é o lado seguro para o cliente — no pior caso ele esconde um ícone.
	 */
	public static final PacketCodec<ByteBuf, ServerFeatures> PACKET_CODEC =
		PacketCodecs.VAR_INT.xmap(ServerFeatures::fromBits, ServerFeatures::toBits);

	private int toBits() {
		int bits = 0;
		boolean[] flags = {locationSharing, chatLocation, radar, photos, messages, photoSharing, flashlight, cobblemon};
		for (int i = 0; i < flags.length; i++) {
			if (flags[i]) {
				bits |= 1 << i;
			}
		}
		return bits;
	}

	private static ServerFeatures fromBits(int bits) {
		return new ServerFeatures(
			(bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0,
			(bits & 16) != 0, (bits & 32) != 0, (bits & 64) != 0, (bits & 128) != 0);
	}
}
