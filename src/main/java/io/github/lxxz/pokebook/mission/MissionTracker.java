package io.github.lxxz.pokebook.mission;

import io.github.lxxz.pokebook.Pokebook;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

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
		advance(player, ObjectiveType.KILL, victim);
	}

	private static void advance(ServerPlayerEntity player, ObjectiveType type, Entity target) {
		MissionProgress progress = player.getAttachedOrCreate(PROGRESS);
		boolean changed = false;
		for (Mission mission : Missions.all()) {
			if (mission.objective() == type && mission.target().matches(target) && progress.advance(mission)) {
				changed = true;
			}
		}
		// O anexo só é marcado como sujo ao ser reatribuído; mutar o objeto não basta.
		if (changed) {
			player.setAttached(PROGRESS, progress);
		}
	}
}
