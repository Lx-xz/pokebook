package io.github.lxxz.pokebook.notify;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * De onde vem um aviso.
 *
 * <p>Serve para o cliente escolher o ícone, e para decidir o que o "não perturbe" cala: um
 * alarme que o próprio jogador programou toca mesmo assim — é a exceção que qualquer
 * celular faz, e sem ela ninguém usaria o modo de noite.
 */
public enum NotificationKind {
	MISSION,
	CALL,
	CONTACT,
	LOCATION,
	ALARM,
	PHOTO,
	// Sempre no fim: o que viaja é o ordinal, e inserir no meio mudaria o significado dos
	// que já existem para um cliente de versão anterior.
	MESSAGE;

	/** Viaja o ordinal; índice desconhecido vira aviso de missão, que é o mais inofensivo. */
	public static final PacketCodec<ByteBuf, NotificationKind> PACKET_CODEC =
		PacketCodecs.VAR_INT.xmap(NotificationKind::byIndex, NotificationKind::ordinal);

	private static NotificationKind byIndex(int index) {
		NotificationKind[] values = values();
		return index >= 0 && index < values.length ? values[index] : MISSION;
	}

	/** Toca mesmo com o "não perturbe" ligado. */
	public boolean bypassesDoNotDisturb() {
		return this == ALARM;
	}
}
