package io.github.lxxz.pokebook.ranking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.mission.Mission;
import io.github.lxxz.pokebook.mission.MissionProgress;
import io.github.lxxz.pokebook.mission.MissionTracker;
import io.github.lxxz.pokebook.mission.Missions;
import io.github.lxxz.pokebook.network.RankingEntry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O ranking de missões de todos os tempos, incluindo quem está offline.
 *
 * <p><b>É o armazenamento de mundo que faltava.</b> O progresso de missão mora no anexo do
 * jogador, que só existe com ele conectado — a aba social só mostra os presentes por isso.
 * Aqui guarda-se, no próprio mundo, um resumo por jogador que sobrevive a ele sair.
 *
 * <p><b>Anexo de mundo, e não um {@code PersistentState} escrito à mão.</b> A API de anexos
 * do Fabric aceita o mundo como dono, e por baixo ela mesma usa um {@code PersistentState}
 * — ver {@code ServerWorldMixin} na Fabric API. Usar o mesmo mecanismo do progresso de
 * missão poupa um segundo jeito de salvar dado, com NBT montado à mão, que alguém teria de
 * aprender. O dono é sempre o <b>mundo principal</b>: o ranking é do servidor, não de uma
 * dimensão.
 *
 * <p>Só o número de concluídas é guardado, e não o progresso inteiro: é o que o ranking
 * mostra, e o registro de cada jogador continua no save dele. O número de quem está offline
 * fica como estava da última vez que ele esteve aqui — se um {@code /reload} remover uma
 * missão, a conta dele só se corrige quando ele voltar.
 */
public final class Ranking {
	/** Uma linha guardada: o último nome conhecido e quantas concluiu. */
	public record Standing(String name, int completed) {
		static final Codec<Standing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("name").forGetter(Standing::name),
			Codec.INT.fieldOf("completed").forGetter(Standing::completed)
		).apply(instance, Standing::new));
	}

	/** Todas as linhas. Imutável pelo mesmo motivo de {@code PhoneData}: só reatribuir marca sujo. */
	public record Board(Map<UUID, Standing> standings) {
		// Chave em texto: mapa em NBT exige chave de texto, e o codec padrão de UUID é um
		// vetor de inteiros.
		static final Codec<Board> CODEC = Codec.unboundedMap(Uuids.STRING_CODEC, Standing.CODEC)
			.xmap(Board::new, Board::standings);

		static final Board EMPTY = new Board(Map.of());

		public Board {
			standings = Map.copyOf(standings);
		}
	}

	public static final AttachmentType<Board> BOARD = AttachmentRegistry.create(
		Identifier.of(Pokebook.MOD_ID, "ranking"),
		builder -> builder
			.initializer(() -> Board.EMPTY)
			.persistent(Board.CODEC)
	);

	private Ranking() {
	}

	/** Força o carregamento da classe, e com ela o registro do anexo, no início do mod. */
	public static void register() {
	}

	/**
	 * Atualiza a linha deste jogador com o nome e a contagem de agora.
	 *
	 * <p>Chamado ao entrar (acompanha troca de apelido) e quando uma missão é concluída.
	 * Não reatribui se nada mudou — reatribuir marca o mundo como sujo, e isso é escrita em
	 * disco no próximo salvamento.
	 */
	public static void update(ServerPlayerEntity player) {
		ServerWorld overworld = player.server.getOverworld();
		Board board = overworld.getAttachedOrCreate(BOARD);
		Standing now = new Standing(player.getGameProfile().getName(), completedBy(player));
		if (now.equals(board.standings().get(player.getUuid()))) {
			return;
		}
		Map<UUID, Standing> updated = new HashMap<>(board.standings());
		updated.put(player.getUuid(), now);
		overworld.setAttached(BOARD, new Board(updated));
	}

	/**
	 * O ranking inteiro, do que concluiu mais para o que concluiu menos.
	 *
	 * <p>Quem está conectado entra com o número de agora, calculado na hora; quem não está,
	 * com o guardado. Empate desfeito pelo nome, para a ordem não dançar entre consultas.
	 */
	public static List<RankingEntry> snapshot(MinecraftServer server) {
		Board board = server.getOverworld().getAttachedOrCreate(BOARD);
		List<RankingEntry> entries = new ArrayList<>();
		Set<UUID> online = new HashSet<>();

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			online.add(player.getUuid());
			entries.add(new RankingEntry(player.getGameProfile().getName(), completedBy(player), true));
		}
		board.standings().forEach((uuid, standing) -> {
			if (!online.contains(uuid)) {
				entries.add(new RankingEntry(standing.name(), standing.completed(), false));
			}
		});

		entries.sort(Comparator.comparingInt(RankingEntry::completed).reversed()
			.thenComparing(RankingEntry::name, String.CASE_INSENSITIVE_ORDER));
		return entries;
	}

	private static int completedBy(ServerPlayerEntity player) {
		MissionProgress progress = player.getAttachedOrCreate(MissionTracker.PROGRESS);
		int completed = 0;
		for (Mission mission : Missions.all()) {
			if (progress.isComplete(mission)) {
				completed++;
			}
		}
		return completed;
	}
}
