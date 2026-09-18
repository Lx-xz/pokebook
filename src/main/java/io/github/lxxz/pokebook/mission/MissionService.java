package io.github.lxxz.pokebook.mission;

import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.SocialEntry;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
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
				mission.title(),
				progress.count(mission.id()),
				mission.required(),
				mission.reward(),
				progress.isClaimed(mission.id())
			));
		}
		return entries;
	}

	/**
	 * O quadro de todos os jogadores **conectados**, do que fez mais para o que fez menos.
	 *
	 * <p>Só os conectados, e isso é limitação consciente: o progresso de quem está offline
	 * mora no arquivo de save do jogador, e lê-lo exigiria abrir um arquivo por jogador a
	 * cada consulta. O degrau que a aba social entrega hoje — ver quem está na frente
	 * agora — não paga esse custo. Um placar histórico é outra funcionalidade.
	 *
	 * <p>Manda um resumo por jogador, não a lista de missões dele: é o que a pergunta
	 * pede, e não cresce com o número de missões.
	 */
	public static List<SocialEntry> socialSnapshot(MinecraftServer server) {
		int total = Missions.count();
		List<SocialEntry> entries = new ArrayList<>();

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			MissionProgress progress = player.getAttachedOrCreate(MissionTracker.PROGRESS);
			int completed = 0;
			int claimed = 0;
			for (Mission mission : Missions.all()) {
				if (progress.isComplete(mission)) {
					completed++;
				}
				if (progress.isClaimed(mission.id())) {
					claimed++;
				}
			}
			entries.add(new SocialEntry(player.getGameProfile().getName(), completed, claimed, total));
		}

		// Quem concluiu mais primeiro; empate desfeito pelo nome, para a ordem não dançar
		// entre duas consultas seguidas com os mesmos números.
		entries.sort(Comparator.comparingInt(SocialEntry::completed).reversed()
			.thenComparing(SocialEntry::name));
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
		// Uma missão de datapack pode não dar nada; nesse caso não há o que caber.
		if (mission.hasReward() && player.getInventory().getEmptySlot() < 0) {
			player.sendMessage(Text.translatable("message.pokebook.inventory_full"), true);
			return;
		}

		if (mission.hasReward()) {
			player.getInventory().insertStack(mission.reward());
		}
		progress.markClaimed(missionId);
		// O anexo só é marcado como sujo ao ser reatribuído.
		player.setAttached(MissionTracker.PROGRESS, progress);

		ServerPlayNetworking.send(player, new MissionsUpdatePayload(snapshot(player)));
	}
}
