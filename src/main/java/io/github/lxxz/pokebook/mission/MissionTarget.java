package io.github.lxxz.pokebook.mission;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * O que acabou de acontecer, na forma que os alvos sabem interrogar.
 *
 * <p>Existe por uma assimetria da API do Cobblemon: matar entrega uma {@link Entity},
 * mas capturar e vencer entregam um <em>Pokémon</em> — o objeto de dados, não a entidade.
 * Um alvo que só soubesse olhar entidade não decidiria nada sobre uma captura.
 *
 * <p><b>Tudo aqui é tipo do Minecraft ou do Java</b> — {@link Identifier}, {@link String}
 * — e é isso que mantém o sistema de missões inteiro livre do Cobblemon. Quem traduz
 * {@code ElementalType} em {@code "fire"} e as etiquetas da espécie em {@code "gen1"} é a
 * classe de integração, e daí para dentro é só texto. Uma missão por tipo ou por geração
 * <b>carrega sem o Cobblemon instalado</b>; ela apenas nunca progride.
 *
 * <p>Geração é etiqueta, não campo: o Cobblemon não guarda um número de geração, guarda um
 * conjunto de rótulos na espécie, entre eles {@code gen1}. Modelar como etiqueta é
 * espelhar o dado real em vez de inventar um paralelo.
 */
public record MissionTarget(
	@Nullable Entity entity,
	@Nullable Identifier species,
	Set<String> elements,
	Set<String> labels
) {
	public static MissionTarget ofEntity(Entity entity) {
		return new MissionTarget(entity, null, Set.of(), Set.of());
	}

	public static MissionTarget ofPokemon(Identifier species, Set<String> elements, Set<String> labels) {
		return new MissionTarget(null, species, Set.copyOf(elements), Set.copyOf(labels));
	}
}
