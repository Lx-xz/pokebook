package io.github.lxxz.pokebook.call;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Em que pé está a ligação de um jogador.
 *
 * <p>São quatro estados e não um booleano "falando" pelo mesmo motivo que a tela do bloco
 * tem quatro níveis: o meio do caminho existe e precisa de nome. Chamar e ser chamado são
 * situações diferentes — uma oferece desligar, a outra oferece atender — e a interface se
 * monta a partir daqui.
 *
 * <p>O estado é <b>do jogador</b>, não da ligação: cada lado recebe o seu. Quem liga vê
 * {@link #DIALING} enquanto quem é chamado vê {@link #RINGING}, e é a mesma ligação.
 */
public enum CallState {
	/** Nenhuma ligação. É o estado em que se escolhe para quem ligar. */
	IDLE,

	/** Liguei para alguém e estou esperando atenderem. */
	DIALING,

	/** Alguém está me ligando. O aparelho toca até eu atender, recusar ou o tempo acabar. */
	RINGING,

	/** Ligação em curso. É o único estado em que o áudio atravessa. */
	ACTIVE;

	/**
	 * O codec viaja o ordinal, não o nome: são quatro valores e o pacote é mandado de
	 * novo a cada transição.
	 *
	 * <p>Índice desconhecido vira {@link #IDLE} em vez de estourar. Um cliente de versão
	 * diferente é um caso real, e o pior que pode acontecer é ele achar que não está em
	 * ligação nenhuma — o servidor continua sendo quem decide.
	 */
	public static final PacketCodec<ByteBuf, CallState> PACKET_CODEC =
		PacketCodecs.VAR_INT.xmap(CallState::byIndex, CallState::ordinal);

	private static CallState byIndex(int index) {
		CallState[] values = values();
		return index >= 0 && index < values.length ? values[index] : IDLE;
	}
}
