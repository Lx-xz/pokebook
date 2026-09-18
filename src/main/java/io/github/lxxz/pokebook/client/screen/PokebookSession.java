package io.github.lxxz.pokebook.client.screen;

import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * De onde esta tela foi aberta.
 *
 * <p>Existe para as telas não carregarem três parâmetros soltos que se repetem em cada
 * construtor e em cada navegação. Mais importante: é o único lugar que sabe a diferença
 * entre o pokébook e o poképhone, e as telas não precisam saber qual está aberto.
 *
 * <p>{@code pos} ausente significa <b>aparelho de bolso</b>. Disso decorrem três coisas:
 * não há bloco para avisar ao fechar, não há distância máxima a respeitar — ninguém se
 * afasta do próprio bolso — e a moldura é <b>em pé</b> em vez de deitada.
 *
 * <p>Portátil e em pé andam juntos hoje porque só há dois aparelhos e eles diferem nas
 * duas coisas ao mesmo tempo. Se um dia houver um aparelho portátil deitado, isto vira
 * dois campos; por ora um campo só evita inventar uma distinção que ninguém usa.
 */
public record PokebookSession(Optional<BlockPos> pos, String nick) {
	public static PokebookSession atBlock(BlockPos pos, String nick) {
		return new PokebookSession(Optional.of(pos), nick);
	}

	public static PokebookSession inHand(String nick) {
		return new PokebookSession(Optional.empty(), nick);
	}

	/** Sem bloco por trás: não avisa ao fechar nem fecha por distância. */
	public boolean portable() {
		return pos.isEmpty();
	}

	/** Moldura em pé. Cabem mais linhas de lista; cabe menos texto por linha. */
	public boolean portrait() {
		return portable();
	}
}
