package io.github.lxxz.pokebook.mission;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * O que a missão pede que você faça.
 *
 * <p>Este é um dos dois eixos de extensão do sistema; o outro é {@link TargetMatcher},
 * que decide <em>em quem</em> vale. Separar os dois é o que permite combinar "matar" com
 * "qualquer coisa do tipo voador" sem escrever uma classe para cada combinação.
 *
 * <p>Por ora só {@link #KILL} está implementado — é o único que funciona sem o Cobblemon,
 * e por isso é o único testável em qualquer máquina. Os outros entram quando o Cobblemon
 * estiver no projeto, e cada um é um evento diferente: capturar e vencer batalha não são
 * a mesma coisa que matar.
 *
 * <p>O nome em JSON é declarado à parte do nome da constante: {@code name()} é detalhe de
 * implementação Java, e renomear a constante não pode quebrar datapacks já escritos.
 */
public enum ObjectiveType implements StringIdentifiable {
	/** Matar uma entidade. Vale para vaca, zumbi e também para um Pokémon morto fora de batalha. */
	KILL("kill");
	// CAPTURE     — evento POKEMON_CAPTURED do Cobblemon
	// BATTLE_WIN  — vencer uma batalha do Cobblemon

	public static final Codec<ObjectiveType> CODEC = StringIdentifiable.createCodec(ObjectiveType::values);

	private final String name;

	ObjectiveType(String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}

	/** Ex.: {@code objective.pokebook.kill}. O sufixo {@code .one} é a forma sem quantidade. */
	public String translationKey() {
		return "objective.pokebook." + name;
	}
}
