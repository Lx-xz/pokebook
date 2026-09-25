package io.github.lxxz.pokebook.mission;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.group.Groups;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import io.github.lxxz.pokebook.notify.NotificationKind;
import io.github.lxxz.pokebook.notify.Notifications;
import io.github.lxxz.pokebook.ranking.Ranking;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Liga os eventos do jogo ao progresso das missões.
 *
 * <p>Hoje escuta só morte de entidade, que é vanilla e portanto funciona em qualquer
 * máquina. Quando o Cobblemon entrar, captura e vitória em batalha viram outros dois
 * ganchos — mas precisam morar numa classe própria, tocada só depois de confirmar que o
 * Cobblemon está presente. Mencionar classe dele aqui derrubaria o jogo sem o mod
 * instalado, pelo mesmo motivo que classe de cliente derruba o servidor dedicado.
 */
public final class MissionTracker {
	// AttachmentRegistry.builder() está @Deprecated; o substituto é o create(id, consumidor),
	// que recebe o Identifier na frente em vez de terminar com buildAndRegister(id).
	public static final AttachmentType<MissionProgress> PROGRESS =
		AttachmentRegistry.create(
			Identifier.of(Pokebook.MOD_ID, "mission_progress"),
			builder -> builder
				.initializer(MissionProgress::new)
				.persistent(MissionProgress.CODEC)
				// Sem isto o jogador perde tudo ao morrer: o respawn cria uma entidade nova.
				.copyOnDeath()
		);

	private MissionTracker() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {
				onKill(player, entity);
			}
		});
	}

	private static void onKill(ServerPlayerEntity player, LivingEntity victim) {
		advance(player, ObjectiveType.KILL, MissionTarget.ofEntity(victim));
	}

	/**
	 * Um Pokémon foi capturado.
	 *
	 * <p>Público porque quem chama é a classe de integração com o Cobblemon, que vive
	 * noutro pacote e só é tocada quando o mod está presente. O Pokémon chega já traduzido
	 * para {@link MissionTarget} — nenhum tipo do Cobblemon atravessa esta fronteira.
	 */
	public static void onCapture(ServerPlayerEntity player, MissionTarget pokemon) {
		advance(player, ObjectiveType.CAPTURE, pokemon);
	}

	/** Uma batalha foi vencida. O alvo é cada Pokémon derrotado. */
	public static void onBattleWin(ServerPlayerEntity player, MissionTarget defeated) {
		advance(player, ObjectiveType.BATTLE_WIN, defeated);
	}

	private static void advance(ServerPlayerEntity player, ObjectiveType type, MissionTarget target) {
		MissionProgress progress = player.getAttachedOrCreate(PROGRESS);
		boolean changed = false;
		List<Mission> justCompleted = new ArrayList<>();
		for (Mission mission : Missions.all()) {
			// A cooperativa conta no grupo, não aqui. Ver Groups.
			if (mission.cooperative()) {
				if (mission.objective() == type && mission.target().matches(target)) {
					Groups.advance(player, mission);
				}
				continue;
			}
			if (mission.objective() == type && mission.target().matches(target) && progress.advance(mission)) {
				changed = true;
				// advance() só devolve true quando a contagem sobe sem passar do necessário,
				// então "completa depois de avançar" quer dizer "completou agora".
				if (progress.isComplete(mission)) {
					justCompleted.add(mission);
				}
			}
		}
		if (!changed) {
			return;
		}

		// O anexo só é marcado como sujo ao ser reatribuído; mutar o objeto não basta.
		player.setAttached(PROGRESS, progress);

		// Atualização ao vivo: o cliente fica sabendo na hora, com ou sem tela aberta. Antes
		// o retrato só saía ao abrir o pokébook; a missão acompanhada no HUD precisa andar
		// enquanto o jogador joga, e é a mesma lista — são poucas missões, sai inteira.
		ServerPlayNetworking.send(player, new MissionsUpdatePayload(MissionService.snapshot(player)));

		for (Mission mission : justCompleted) {
			Notifications.send(player, NotificationKind.MISSION,
				Text.translatable("notification.pokebook.mission.completed"), mission.title());
		}
		if (!justCompleted.isEmpty()) {
			Ranking.update(player);
		}
	}
}
