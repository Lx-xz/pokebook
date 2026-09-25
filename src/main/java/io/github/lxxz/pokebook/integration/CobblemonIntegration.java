package io.github.lxxz.pokebook.integration;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.mission.MissionTarget;
import io.github.lxxz.pokebook.mission.MissionTracker;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

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
 * <p>A tradução para o vocabulário do pokébook acontece aqui e só aqui: {@code Pokemon}
 * vira {@link MissionTarget}, {@code ElementalType} vira texto, e as etiquetas da espécie
 * viram {@code String}. Se a API do Cobblemon mudar — e ela muda entre versões menores —
 * o estrago fica contido neste arquivo.
 */
public final class CobblemonIntegration {
	private CobblemonIntegration() {
	}

	public static void register() {
		// A API do Cobblemon é Kotlin, mas oferece sobrecargas com Consumer justamente
		// para consumo em Java; não é preciso lidar com Function1 nem com kotlin.Unit.
		CobblemonEvents.POKEMON_CAPTURED.subscribe(event ->
			MissionTracker.onCapture(event.getPlayer(), describe(event.getPokemon())));

		CobblemonEvents.BATTLE_VICTORY.subscribe(event -> {
			// Uma captura selvagem também encerra a batalha em vitória. Contá-la aqui
			// daria progresso duplo: a mesma ação viraria captura E vitória.
			if (event.getWasWildCapture()) {
				return;
			}

			Set<ServerPlayerEntity> winners = players(event.getWinners());
			if (winners.isEmpty()) {
				return;
			}

			// O alvo da missão é o Pokémon derrotado, não o treinador: "derrotar 3 do tipo
			// fogo" fala dos Pokémon que caíram. Cada um do lado perdedor conta uma vez.
			for (BattleActor loser : event.getLosers()) {
				for (BattlePokemon fallen : loser.getPokemonList()) {
					MissionTarget target = describe(fallen.getOriginalPokemon());
					for (ServerPlayerEntity winner : winners) {
						MissionTracker.onBattleWin(winner, target);
					}
				}
			}
		});

		Pokebook.LOGGER.info("Integração com o Cobblemon ativa.");
	}

	/**
	 * Traduz um Pokémon do Cobblemon para o vocabulário do sistema de missões.
	 *
	 * <p>É aqui que a fronteira de fato acontece: entra {@code Pokemon}, sai só
	 * {@code Identifier} e {@code String}.
	 */
	private static MissionTarget describe(Pokemon pokemon) {
		Set<String> elements = new LinkedHashSet<>();
		for (ElementalType type : pokemon.getSpecies().getTypes()) {
			// showdownId é minúsculo e estável; getName() é nome de exibição e mudaria
			// com o idioma, o que faria a mesma missão casar numa máquina e não noutra.
			elements.add(type.getShowdownId().toLowerCase(Locale.ROOT));
		}

		Set<String> labels = new HashSet<>(pokemon.getSpecies().getLabels());

		return MissionTarget.ofPokemon(pokemon.getSpecies().getResourceIdentifier(), elements, labels);
	}

	/** Os jogadores de verdade entre os atores de um lado da batalha. */
	private static Set<ServerPlayerEntity> players(Iterable<BattleActor> actors) {
		Set<ServerPlayerEntity> players = new LinkedHashSet<>();
		for (BattleActor actor : actors) {
			if (actor instanceof EntityBackedBattleActor<?> backed) {
				Entity entity = backed.getEntity();
				// Um ator pode ser um NPC ou um Pokémon selvagem; só jogador ganha missão.
				if (entity instanceof ServerPlayerEntity player) {
					players.add(player);
				}
			}
		}
		return players;
	}
}
