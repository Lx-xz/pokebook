package io.github.lxxz.pokebook.mission;

import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Casa com um tipo elemental, ex.: {@code fire}. Vale para o tipo primário e o secundário.
 *
 * <p>Sem import do Cobblemon: o tipo chega como texto, normalizado em minúsculas pela
 * classe de integração.
 */
public record ElementMatcher(String element) implements TargetMatcher {
	public ElementMatcher {
		element = element.toLowerCase(Locale.ROOT);
	}

	@Override
	public boolean matches(MissionTarget target) {
		return target.elements().contains(element);
	}

	@Override
	public Text describe() {
		// Chave do próprio Cobblemon. Sem ele, o jogador vê o texto cru, que ainda informa.
		return Text.translatable("cobblemon.type." + element);
	}
}
