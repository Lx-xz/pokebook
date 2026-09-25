package io.github.lxxz.pokebook.phone;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Quem pode ligar para mim.
 *
 * <p>É onde os contatos viram <b>camada de consentimento</b> para a ligação. O padrão é
 * {@link #EVERYONE} porque era o comportamento antes dos contatos existirem, e mudar o
 * padrão faria o telefone de todo mundo parar de tocar num servidor onde ninguém salvou
 * ninguém ainda.
 *
 * <p>O "não perturbe" não é um quarto valor daqui: ele também silencia notificações e
 * suspende o compartilhamento de localização, e misturá-lo com esta escolha obrigaria o
 * jogador a refazer a política toda vez que saísse do modo.
 */
public enum CallPolicy implements StringIdentifiable {
	EVERYONE("everyone"),
	CONTACTS("contacts"),
	NOBODY("nobody");

	public static final Codec<CallPolicy> CODEC = StringIdentifiable.createCodec(CallPolicy::values);

	private final String name;

	CallPolicy(String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}

	public String translationKey() {
		return "screen.pokebook.settings.calls." + name;
	}

	/** O próximo da lista, dando a volta. É o que o botão de ajuste faz a cada clique. */
	public CallPolicy next() {
		CallPolicy[] values = values();
		return values[(ordinal() + 1) % values.length];
	}
}
