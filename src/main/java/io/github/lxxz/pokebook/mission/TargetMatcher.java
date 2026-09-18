package io.github.lxxz.pokebook.mission;

import com.mojang.serialization.Codec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

/**
 * Em quem a missão vale.
 *
 * <p>Trabalha sobre {@link Entity} de propósito: os Pokémon do Cobblemon também são
 * entidades, então o mesmo critério serve para "matar 3 vacas" e, mais tarde, para
 * espécie, tipo ou geração — sem trocar a forma.
 *
 * <p>Hoje existe uma implementação só. Isso é intencional: o seletor genérico só vale a
 * pena depois que houver um segundo caso concreto para comparar.
 */
public interface TargetMatcher {
	/**
	 * Por enquanto um alvo em JSON é só o id de um tipo de entidade:
	 * {@code "target": "minecraft:cow"}.
	 *
	 * <p>Quando houver um segundo tipo de alvo — espécie de Pokémon, tipo elemental —
	 * isto vira um codec despachado por um campo {@code "type"}. A forma de objeto
	 * ({@code {"type": ..., ...}}) é distinguível de uma string, então o atalho de hoje
	 * continua válido e nenhum datapack já escrito quebra.
	 */
	Codec<TargetMatcher> CODEC = EntityTypeMatcher.CODEC.xmap(matcher -> matcher, TargetMatcher::asEntityType);

	boolean matches(Entity entity);

	/** Como o alvo aparece na interface, ex.: "Vaca". */
	Text describe();

	private static EntityTypeMatcher asEntityType(TargetMatcher matcher) {
		if (matcher instanceof EntityTypeMatcher entityType) {
			return entityType;
		}
		// Inalcançável enquanto houver só um tipo de alvo; o dia em que houver outro, é
		// aqui que o codec despachado entra.
		throw new IllegalStateException("Alvo sem forma serializável: " + matcher);
	}
}
