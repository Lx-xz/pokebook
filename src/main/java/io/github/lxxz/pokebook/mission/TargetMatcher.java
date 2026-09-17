package io.github.lxxz.pokebook.mission;

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
	boolean matches(Entity entity);

	/** Como o alvo aparece na interface, ex.: "Vaca". */
	Text describe();
}
