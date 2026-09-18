package io.github.lxxz.pokebook.integration;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.mission.MissionTracker;

/**
 * A única classe do projeto que menciona o Cobblemon.
 *
 * <p><b>Isto é fronteira de classe, não condicional.</b> No Minecraft, código opcional
 * precisa viver numa classe própria: a JVM resolve as referências ao <em>carregar</em> a
 * classe, então um {@code if (temCobblemon)} dentro de um método já falharia — a classe
 * que o contém não carregaria. É o mesmo motivo pelo qual código de cliente derruba um
 * servidor dedicado, e é o terceiro lugar onde este padrão aparece no projeto.
 *
 * <p>Quem chama {@link #register()} confere antes se o mod está presente, e nada mais no
 * mod toca este pacote.
 *
 * <p>A tradução para o vocabulário do pokébook acontece aqui e só aqui: o evento entrega
 * um {@code Pokemon}, e o que atravessa para o sistema de missões é o
 * {@code Identifier} da espécie. Se a API do Cobblemon mudar — e ela muda entre versões
 * menores — o estrago fica contido neste arquivo.
 */
public final class CobblemonIntegration {
	private CobblemonIntegration() {
	}

	public static void register() {
		// A API do Cobblemon é Kotlin, mas oferece sobrecargas com Consumer justamente
		// para consumo em Java; não é preciso lidar com Function1 nem com kotlin.Unit.
		CobblemonEvents.POKEMON_CAPTURED.subscribe(event ->
			MissionTracker.onCapture(event.getPlayer(), event.getPokemon().getSpecies().getResourceIdentifier()));

		Pokebook.LOGGER.info("Integração com o Cobblemon ativa.");
	}
}
