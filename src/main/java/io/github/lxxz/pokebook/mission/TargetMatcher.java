package io.github.lxxz.pokebook.mission;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;

/**
 * Em quem a missão vale.
 *
 * <p>Este é um dos dois eixos de extensão do sistema; o outro é {@link ObjectiveType}.
 * Separar os dois é o que permite combinar "capturar" com "qualquer Pidgey" sem escrever
 * uma classe por combinação.
 */
public interface TargetMatcher {
	/**
	 * As duas formas aceitas em JSON:
	 *
	 * <pre>
	 * "target": "minecraft:cow"          um tipo de entidade
	 * "target": { "species": "pidgey" }  uma espécie de Pokémon
	 * </pre>
	 *
	 * <p>São só duas, então são duas alternativas e não um codec despachado por um campo
	 * {@code "type"}. O despacho entra quando houver um terceiro — e a forma de objeto já
	 * está aberta para recebê-lo sem invalidar nenhum datapack escrito hoje.
	 */
	Codec<TargetMatcher> CODEC = Codec.either(EntityTypeMatcher.CODEC, SpeciesMatcher.CODEC)
		.xmap(
			either -> either.map(entity -> (TargetMatcher) entity, species -> (TargetMatcher) species),
			matcher -> matcher instanceof EntityTypeMatcher entityType
				? com.mojang.datafixers.util.Either.left(entityType)
				: com.mojang.datafixers.util.Either.right((SpeciesMatcher) matcher)
		);

	boolean matches(MissionTarget target);

	/** Como o alvo aparece na interface, ex.: "Vaca" ou "Pidgey". */
	Text describe();
}
