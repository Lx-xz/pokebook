package io.github.lxxz.pokebook.mission;

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
 */
public enum ObjectiveType {
	/** Matar uma entidade. Vale para vaca, zumbi e também para um Pokémon morto fora de batalha. */
	KILL
	// CAPTURE     — evento POKEMON_CAPTURED do Cobblemon
	// BATTLE_WIN  — vencer uma batalha do Cobblemon
}
