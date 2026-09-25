package io.github.lxxz.pokebook.phone;

import io.github.lxxz.pokebook.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Quem está com um poképhone.
 *
 * <p>É a pergunta que decide se o aparelho pode <b>chamar a atenção</b> de alguém: tocar
 * numa ligação, mostrar uma notificação, desenhar a seta no HUD, disparar um alarme. Sem
 * aparelho no bolso nada disso tem por onde chegar — e avisar alguém que nunca ouviu falar
 * do mod só confundiria. A regra nasceu no toque da ligação e virou de todo mundo.
 *
 * <p>O pokébook (o bloco) não conta. Ele administra, não comunica: está parado numa mesa, e
 * o jogador que está longe dele não tem como ser avisado por ele.
 *
 * <p>Aceita {@link PlayerEntity}, e não só o jogador do servidor, porque o cliente faz a
 * mesma pergunta sobre si mesmo antes de desenhar o HUD. O inventário é sincronizado com o
 * próprio dono, então a resposta nos dois lados é a mesma.
 */
public final class Pokephones {
	private Pokephones() {
	}

	/** Tem um poképhone em algum lugar do inventário, inclusive fora da hotbar. */
	public static boolean carries(PlayerEntity player) {
		return player.getInventory().contains(stack -> stack.isOf(ModItems.POKEPHONE));
	}
}
