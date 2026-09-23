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
	/**
	 * Entre um nível e o seguinte. Igual nos dois sentidos: três níveis a 2 ticks dão
	 * 0,3 s de transição, tanto para acender quanto para apagar.
	 *
	 * <p>O apagar já foi lento de propósito — um segundo por nível, precedido de três
	 * segundos parado, imitando o fade de um monitor de verdade. Na prática ficou
	 * arrastado: fechar a tela e o bloco continuar aceso parecia defeito, não estilo.
	 */
	private static final int FADE_STEP_TICKS = 2;

	/**
	 * Se a tela percorre os níveis ou salta direto ao alvo.
	 *
	 * <p>Desligada enquanto a v2 tem só duas artes. Percorrer com duas artes produzia uma
	 * assimetria incômoda: ao ligar, os níveis 1 e 2 mostram a arte APAGADA, então a tela
	 * só acendia no terceiro passo; ao desligar, o primeiro passo já saía do nível 3 e era
	 * visível na hora. Ligar parecia lento e desligar instantâneo.
	 *
	 * <p>Com o salto direto, os dois sentidos respondem no mesmo quadro. Quando as quatro
	 * artes existirem, volte para {@code true}.
	 */
	private static final boolean ANIMATION_ENABLED = false;

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

	/**
	 * Este jogador está com algum pokébook aberto agora?
	 *
	 * <p>É o que autoriza o resgate de recompensa. A regra "resgata-se no pokébook, não no
	 * poképhone" precisa ser conferida no <b>servidor</b>: esconder o botão no cliente é
	 * aparência, não regra — o pacote de resgate continua sendo um pacote que qualquer
	 * cliente pode mandar. E o conjunto de espectadores, que já existia para a animação da
	 * tela, é exatamente a informação necessária.
	 */
	public static boolean isViewingBlock(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		for (Entry entry : OPEN.values()) {
			if (entry.viewers.contains(uuid)) {
				return true;
			}
		}
		return false;
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
	 * Saiu o último espectador: a tela começa a apagar imediatamente.
	 *
	 * <p>O "sempre aceso" é conferido dentro de {@link #step}, que relê o alvo a cada
	 * passo — por isso não há ramo para ele aqui.
	 *
	 * <p>A entrada do mapa é mantida de propósito, mesmo sem ninguém olhando: se fosse
	 * removida aqui, reabrir no meio da transição faria {@link #open} ler o bloco ainda
	 * aceso e concluir que o "sempre aceso" estava ligado — e a tela nunca mais apagaria.
	 */
	private static void beginShutdown(Key key, Entry entry) {
		step(key);
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

		// Sem animação não há quadro intermediário que valha a pena: vai direto ao alvo.
		int next = ANIMATION_ENABLED ? current + Integer.signum(target - current) : target;
		world.setBlockState(pos, state.with(PokebookBlock.SCREEN, next), Block.NOTIFY_ALL);

		// Só no primeiro passo de cada transição: a animação tem quatro níveis, e um som
		// por nível viraria uma metralhadora. O som acompanha a intenção, não o quadro.
		if (current == PokebookBlock.SCREEN_OFF) {
			PokebookSounds.playAt(world, pos, PokebookSounds.SCREEN_ON, 0.6f, 1.0f);
		} else if (current == PokebookBlock.SCREEN_ON && next < current) {
			PokebookSounds.playAt(world, pos, PokebookSounds.SCREEN_OFF, 0.6f, 1.0f);
		}

		if (next != target) {
			schedule(key, FADE_STEP_TICKS);
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
