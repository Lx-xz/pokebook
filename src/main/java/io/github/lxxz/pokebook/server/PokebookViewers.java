package io.github.lxxz.pokebook.server;

import io.github.lxxz.pokebook.block.PokebookBlock;
import io.github.lxxz.pokebook.sound.PokebookSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Quem está com cada pokébook aberto, e a transição da tela.
 *
 * <p>A tela tem quatro níveis, não um booleano, para poder acender e apagar aos poucos.
 * O nível guarda duas intenções distintas: "o jogador deixou isto aceso" e "alguém está
 * com a tela aberta". Elas precisam ser separadas, senão fechar a interface apagaria o
 * pokébook que tinha sido aceso de propósito.
 *
 * <p>A regra é: <b>alvo = 3 se há espectador ou o manual está ligado, senão 0</b>.
 * Enquanto ninguém olha e nada está em transição, o nível <em>é</em> o estado manual — e
 * é assim que ele sobrevive a recarregar o mundo, já que não temos BlockEntity.
 *
 * <p>A contagem de espectadores resolve o caso de dois jogadores: a tela só começa a
 * apagar quando o <em>último</em> fecha, e não na cara de quem ainda está olhando.
 *
 * <p>A animação é feita com ticks agendados de bloco encadeados: cada tick anda um nível
 * e agenda o próximo. Não é BlockEntity — o bloco continua burro, e o agendamento é
 * salvo junto com o chunk.
 *
 * <p>Isto vive só em memória e de propósito. Se o servidor cair no meio de uma transição,
 * a tela fica no nível em que estava — o shift+clique resolve. Persistir custaria bem
 * mais do que o problema vale.
 *
 * <p>Tudo aqui roda na thread do servidor, então as coleções não precisam ser sincronizadas.
 */
public final class PokebookViewers {
	/** Espera antes de <em>começar</em> a apagar, depois que o último espectador sai. */
	public static final int SHUTDOWN_DELAY_TICKS = 60;

	/** Entre um nível e o seguinte ao apagar: um segundo por nível, como uma tela de verdade. */
	private static final int FADE_OUT_STEP_TICKS = 20;

	/** Entre um nível e o seguinte ao acender — rápido, para o clique parecer responsivo. */
	private static final int FADE_IN_STEP_TICKS = 2;

	/** Posição sozinha não identifica um bloco: as mesmas coordenadas existem em cada dimensão. */
	private record Key(ServerWorld world, BlockPos pos) {
	}

	/** Espectadores de um pokébook, mais o estado manual que a tela está encobrindo. */
	private static final class Entry {
		final Set<UUID> viewers = new HashSet<>();
		boolean manuallyLit;
	}

	private static final Map<Key, Entry> OPEN = new HashMap<>();

	private PokebookViewers() {
	}

	public static void open(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
		Key key = new Key(world, pos.toImmutable());
		Entry entry = OPEN.get(key);
		if (entry == null) {
			entry = new Entry();
			// Primeiro a abrir: enquanto não havia ninguém, o nível era o estado manual.
			entry.manuallyLit = level(world, pos) == PokebookBlock.SCREEN_ON;
			OPEN.put(key, entry);
		}
		entry.viewers.add(player.getUuid());
		step(key);
	}

	public static void close(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
		Key key = new Key(world, pos.toImmutable());
		Entry entry = OPEN.get(key);
		if (entry == null) {
			return;
		}
		entry.viewers.remove(player.getUuid());
		if (entry.viewers.isEmpty()) {
			beginShutdown(key, entry);
		}
	}

	/**
	 * Shift+clique: liga ou desliga o "sempre aceso", independente da interface.
	 *
	 * <p>Com alguém olhando, a tela não pode apagar na hora — a escolha é registrada e
	 * vale quando a última tela fechar. Na prática isso quase nunca acontece: com a sua
	 * própria tela aberta você não consegue clicar no bloco.
	 */
	public static void toggleManual(ServerWorld world, BlockPos pos, BlockState state) {
		Key key = new Key(world, pos.toImmutable());
		Entry entry = OPEN.get(key);
		if (entry == null) {
			// Cria uma entrada só para carregar a intenção durante a transição; ela é
			// descartada sozinha quando a animação termina e ninguém está olhando.
			entry = new Entry();
			entry.manuallyLit = state.get(PokebookBlock.SCREEN) != PokebookBlock.SCREEN_ON;
			OPEN.put(key, entry);
		} else {
			entry.manuallyLit = !entry.manuallyLit;
		}
		if (entry.viewers.isEmpty() && !entry.manuallyLit) {
			beginShutdown(key, entry);
		} else {
			step(key);
		}
	}

	/** Chamado quando o jogador desconecta: ele não vai mandar o pacote de fechamento. */
	public static void removePlayer(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();

		// Coleta primeiro, age depois: os passos mexem no mapa, e alterá-lo durante a
		// iteração dá ConcurrentModificationException.
		List<Key> emptied = new ArrayList<>();
		for (Map.Entry<Key, Entry> e : OPEN.entrySet()) {
			if (e.getValue().viewers.remove(uuid) && e.getValue().viewers.isEmpty()) {
				emptied.add(e.getKey());
			}
		}

		for (Key key : emptied) {
			Entry entry = OPEN.get(key);
			if (entry != null) {
				beginShutdown(key, entry);
			}
		}
	}

	/** Chamado quando o bloco deixa de existir: não adianta animar o que não está mais lá. */
	public static void forget(ServerWorld world, BlockPos pos) {
		OPEN.remove(new Key(world, pos.toImmutable()));
	}

	/** Um passo da animação, disparado pelo tick agendado. */
	public static void stepScreen(ServerWorld world, BlockPos pos) {
		step(new Key(world, pos.toImmutable()));
	}

	/**
	 * Saiu o último espectador: a tela não começa a apagar na hora.
	 *
	 * <p>A entrada é mantida de propósito, mesmo sem ninguém olhando. Se fosse removida
	 * aqui, reabrir durante a espera faria {@link #open} ler o bloco ainda aceso e
	 * concluir que o "sempre aceso" estava ligado — e a tela nunca mais apagaria.
	 */
	private static void beginShutdown(Key key, Entry entry) {
		if (entry.manuallyLit) {
			step(key);
			return;
		}
		schedule(key, SHUTDOWN_DELAY_TICKS);
	}

	/**
	 * Anda um nível na direção do alvo e agenda o próximo, se ainda não chegou.
	 *
	 * <p>Confere o alvo a cada passo em vez de guardá-lo: durante a animação o jogador
	 * pode ter reaberto a tela ou ligado o "sempre aceso", e nesse caso a transição
	 * precisa inverter no meio do caminho em vez de terminar no lugar errado.
	 */
	private static void step(Key key) {
		ServerWorld world = key.world();
		BlockPos pos = key.pos();
		BlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof PokebookBlock)) {
			OPEN.remove(key);
			return;
		}

		Entry entry = OPEN.get(key);
		// Sem entrada só se chega recarregando o chunk no meio de uma transição; nesse
		// caso o destino certo é apagado, porque manual aceso não agenda tick nenhum.
		int target = entry == null || (entry.viewers.isEmpty() && !entry.manuallyLit)
			? PokebookBlock.SCREEN_OFF
			: PokebookBlock.SCREEN_ON;

		int current = state.get(PokebookBlock.SCREEN);
		if (current == target) {
			// Chegou. A entrada só existe para carregar intenção durante a animação e
			// enquanto alguém olha; sem nada disso, o próprio nível guarda o estado.
			if (entry != null && entry.viewers.isEmpty()) {
				OPEN.remove(key);
			}
			return;
		}

		int next = current + Integer.signum(target - current);
		world.setBlockState(pos, state.with(PokebookBlock.SCREEN, next), Block.NOTIFY_ALL);

		// Só no primeiro passo de cada transição: a animação tem quatro níveis, e um som
		// por nível viraria uma metralhadora. O som acompanha a intenção, não o quadro.
		if (current == PokebookBlock.SCREEN_OFF) {
			PokebookSounds.playAt(world, pos, PokebookSounds.SCREEN_ON, 0.6f, 1.0f);
		} else if (current == PokebookBlock.SCREEN_ON && next < current) {
			PokebookSounds.playAt(world, pos, PokebookSounds.SCREEN_OFF, 0.6f, 1.0f);
		}

		if (next != target) {
			schedule(key, target > next ? FADE_IN_STEP_TICKS : FADE_OUT_STEP_TICKS);
		} else if (entry != null && entry.viewers.isEmpty()) {
			OPEN.remove(key);
		}
	}

	private static void schedule(Key key, int delayTicks) {
		key.world().scheduleBlockTick(key.pos(), key.world().getBlockState(key.pos()).getBlock(), delayTicks);
	}

	private static int level(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.getBlock() instanceof PokebookBlock ? state.get(PokebookBlock.SCREEN) : PokebookBlock.SCREEN_OFF;
	}
}
