package io.github.lxxz.pokebook.mission;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Nullable;

/**
 * O que acabou de acontecer, na forma que os alvos sabem interrogar.
 *
 * <p>Existe por uma assimetria da API do Cobblemon: matar entrega uma {@link Entity},
 * mas capturar entrega um <em>Pokémon</em> — o objeto de dados, não a entidade, que a
 * essa altura já saiu do mundo. Um alvo que só soubesse olhar entidade não conseguiria
 * decidir nada sobre uma captura.
 *
 * <p>A peça-chave é a espécie viajar como {@link Identifier} simples, e não como classe
 * do Cobblemon. É isso que mantém <b>todo</b> o sistema de missões livre dele: quem
 * extrai o id é a classe de integração, e a partir daí é só um identificador como
 * qualquer outro. Uma missão de espécie até carrega sem o Cobblemon instalado — ela
 * apenas nunca progride, em vez de quebrar o carregamento do datapack.
 */
public record MissionTarget(@Nullable Entity entity, @Nullable Identifier species) {
	public static MissionTarget ofEntity(Entity entity) {
		return new MissionTarget(entity, null);
	}

	public static MissionTarget ofSpecies(Identifier species) {
		return new MissionTarget(null, species);
	}
}
