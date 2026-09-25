package io.github.lxxz.pokebook.integration;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.storage.pc.link.PCLink;
import com.cobblemon.mod.common.api.storage.pc.link.PCLinkManager;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.net.messages.client.storage.pc.OpenPCPacket;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.battle.BattleChallenges;
import io.github.lxxz.pokebook.block.PokebookBlock;
import io.github.lxxz.pokebook.network.OpenPcPayload;
import io.github.lxxz.pokebook.server.PokebookViewers;
import kotlin.Unit;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import io.github.lxxz.pokebook.mission.MissionTarget;
import io.github.lxxz.pokebook.mission.MissionTracker;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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

		// PC remoto: só o pokébook oferece, e só enquanto o jogador estiver perto dele.
		ServerPlayNetworking.registerGlobalReceiver(OpenPcPayload.ID, (payload, context) ->
			openPc(context.player(), payload.pos()));

		// Desafio de batalha pelo celular. O convite é nosso; só o começo da batalha é dele.
		BattleChallenges.setStarter(CobblemonIntegration::startBattle);

		Pokebook.LOGGER.info("Integração com o Cobblemon ativa.");
	}

	/** A que distância do pokébook o PC continua aberto. A mesma folga de baú e fornalha. */
	private static final double PC_MAX_DISTANCE = 8.0;

	/**
	 * Abre o PC do jogador a partir do pokébook.
	 *
	 * <p>O Cobblemon já foi feito para isto: o {@code PCLinkManager} existe justamente para
	 * um bloco customizado dar acesso ao PC, e a tela do PC só deixa mexer enquanto o
	 * {@link PCLink} disser que pode. O nosso diz que pode enquanto o jogador estiver perto
	 * <b>deste</b> pokébook e ele continuar lá.
	 *
	 * <p>⚠️ <b>A permissão é por distância, e não por "estar com o pokébook aberto".</b> Abrir
	 * a tela do PC fecha a nossa, e fechar a nossa avisa o servidor — se a regra fosse "está
	 * olhando o pokébook", o PC deixaria de ser editável no instante em que aparecesse. É o
	 * mesmo desenho do {@code ProximityPCLink} que o bloco de PC dele usa.
	 */
	private static void openPc(ServerPlayerEntity player, BlockPos pos) {
		// O pedido tem de vir de quem está de fato com um pokébook aberto: sem isto, o
		// poképhone viraria PC de bolso mandando o pacote com uma posição qualquer.
		if (!PokebookViewers.isViewingBlock(player)) {
			return;
		}
		ServerWorld world = player.getServerWorld();
		if (!isPokebookNearby(player, world, pos)) {
			return;
		}

		PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
		PCLinkManager.INSTANCE.addLink(new PCLink(pc, player.getUuid()) {
			@Override
			public boolean isPermitted(ServerPlayerEntity user) {
				boolean ok = isPokebookNearby(user, world, pos);
				if (!ok) {
					PCLinkManager.INSTANCE.removeLink(user.getUuid());
				}
				return ok;
			}
		});
		new OpenPCPacket(pc).sendToPlayer(player);
	}

	private static boolean isPokebookNearby(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		return player.getServerWorld() == world
			&& player.getPos().isInRange(Vec3d.ofCenter(pos), PC_MAX_DISTANCE)
			&& world.getBlockState(pos).getBlock() instanceof PokebookBlock;
	}

	/**
	 * Começa a batalha de um desafio aceito.
	 *
	 * <p>{@code pvp1v1} com os valores padrão dele: formato de simples, primeiro Pokémon da
	 * equipe à frente, equipes de verdade (não cópias). O Cobblemon confere se os dois
	 * podem batalhar agora — já em batalha, sem Pokémon — e devolve erro se não.
	 */
	private static boolean startBattle(ServerPlayerEntity challenger, ServerPlayerEntity challenged) {
		boolean[] started = {true};
		// ifErrored recebe uma função Kotlin, que em Java é um lambda que devolve Unit.
		BattleBuilder.INSTANCE.pvp1v1(challenged, challenger).ifErrored(error -> {
			started[0] = false;
			return Unit.INSTANCE;
		});
		return started[0];
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
