package io.github.lxxz.pokebook.mission;

import net.minecraft.text.Text;

/**
 * Casa com uma geração de Pokémon, ex.: {@code 1}.
 *
 * <p>O Cobblemon não guarda um número de geração: guarda <b>etiquetas</b> na espécie, e
 * uma delas é {@code gen1}. Esta classe é açúcar sobre isso — o JSON aceita um número,
 * que vira a etiqueta correspondente. Se um dia quisermos casar com {@code legendary} ou
 * {@code paradox}, é o mesmo mecanismo com outro nome.
 */
public record GenerationMatcher(int generation) implements TargetMatcher {
	@Override
	public boolean matches(MissionTarget target) {
		return target.labels().contains("gen" + generation);
	}

	@Override
	public Text describe() {
		return Text.translatable("target.pokebook.generation", generation);
	}
}
