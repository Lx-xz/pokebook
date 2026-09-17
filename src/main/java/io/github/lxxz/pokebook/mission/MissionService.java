package io.github.lxxz.pokebook.mission;

import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Regras de missão do lado do servidor: o que o jogador vê e o que ele pode resgatar. */
public final class MissionService {
	private MissionService() {
	}

	/** Retrato das missões para este jogador, na ordem em que aparecem na interface. */
	public static List<MissionEntry> snapshot(ServerPlayerEntity player) {
		MissionProgress progress = player.getAttachedOrCreate(MissionTracker.PROGRESS);
		List<MissionEntry> entries = new ArrayList<>();
		for (Mission mission : Missions.all()) {
			entries.add(new MissionEntry(
				mission.id(),
				progress.count(mission.id()),
				mission.required(),
				mission.reward(),
				progress.isClaimed(mission.id())
			));
		}
		return entries;
	}

	/**
	 * Resgata a recompensa, se puder.
	 *
	 * <p>Nada aqui confia no cliente: o pacote traz só o id, e existência, conclusão,
	 * duplicidade e espaço são conferidos aqui.
	 */
	public static void claim(ServerPlayerEntity player, Identifier missionId) {
		Mission mission = Missions.byId(missionId);
		if (mission == null) {
			return;
		}

		MissionProgress progress = player.getAttachedOrCreate(MissionTracker.PROGRESS);
		if (!progress.isComplete(mission) || progress.isClaimed(missionId)) {
			return;
		}

		// A recompensa é única: se cair no chão sobre lava ou no void, some para sempre.
		// Por isso exigimos um espaço livre e recusamos, em vez de dropar aos pés — o
		// jogador libera um slot e clica de novo, sem perder nada. É deliberadamente
		// diferente do vanilla, onde quase tudo é obtenível outra vez.
		if (player.getInventory().getEmptySlot() < 0) {
			player.sendMessage(Text.translatable("message.pokebook.inventory_full"), true);
			return;
		}

		ItemStack reward = mission.reward();
		player.getInventory().insertStack(reward);
		progress.markClaimed(missionId);
		// O anexo só é marcado como sujo ao ser reatribuído.
		player.setAttached(MissionTracker.PROGRESS, progress);

		ServerPlayNetworking.send(player, new MissionsUpdatePayload(snapshot(player)));
	}
}
