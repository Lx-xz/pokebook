package io.github.lxxz.pokebook.server;

import io.github.lxxz.pokebook.block.PokebookBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Quem está com cada pokébook aberto, no servidor.
 *
 * <p>A propriedade {@code LIT} guarda duas intenções distintas: "o jogador deixou isto
 * aceso" e "alguém está com a tela aberta". Elas precisam ser separadas, senão fechar a
 * interface apaga o pokébook que tinha sido aceso de propósito.
 *
 * <p>A regra é: <b>LIT = manual OU tem espectador</b>. Enquanto ninguém está olhando,
 * {@code LIT} <em>é</em> o estado manual — e é assim que ele sobrevive a recarregar o
 * mundo, já que não temos BlockEntity. Quando alguém abre, o manual é guardado aqui e
 * devolvido ao bloco quando o último espectador sai.
 *
 * <p>A contagem também resolve o caso de dois jogadores: a luz só apaga quando o
 * <em>último</em> fecha, e não na cara de quem ainda está olhando.
 *
 * <p>Isto vive só em memória e de propósito. Se o servidor cair com gente olhando, a luz
 * volta acesa e sem espectadores — o shift+clique resolve. Persistir custaria bem mais
 * do que o problema vale.
 *
 * <p>Tudo aqui roda na thread do servidor, então as coleções não precisam ser sincronizadas.
 */
public final class PokebookViewers {
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
			// Primeiro a abrir: enquanto não havia ninguém, LIT era o estado manual.
			entry.manuallyLit = isLit(world, pos);
			OPEN.put(key, entry);
		}
		entry.viewers.add(player.getUuid());
		setLit(world, pos, true);
	}

	public static void close(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
		Key key = new Key(world, pos.toImmutable());
		Entry entry = OPEN.get(key);
		if (entry == null) {
			return;
		}
		entry.viewers.remove(player.getUuid());
		if (entry.viewers.isEmpty()) {
			OPEN.remove(key);
			// Devolve o que o jogador tinha escolhido antes da tela cobrir.
			setLit(world, pos, entry.manuallyLit);
		}
	}

	/**
	 * Shift+clique: liga ou desliga o "sempre aceso", independente da interface.
	 *
	 * <p>Com alguém olhando, a luz não pode apagar na hora — mas a escolha é registrada e
	 * vale assim que a última tela fechar. Na prática isso quase nunca acontece: com a sua
	 * própria tela aberta você não consegue clicar no bloco, então só outro jogador chega
	 * nesse caso.
	 */
	public static void toggleManual(ServerWorld world, BlockPos pos, BlockState state) {
		Entry entry = OPEN.get(new Key(world, pos.toImmutable()));
		if (entry != null) {
			entry.manuallyLit = !entry.manuallyLit;
			return;
		}
		setLit(world, pos, !state.get(PokebookBlock.LIT));
	}

	/** Chamado quando o jogador desconecta: ele não vai mandar o pacote de fechamento. */
	public static void removePlayer(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		Iterator<Map.Entry<Key, Entry>> it = OPEN.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Key, Entry> e = it.next();
			if (e.getValue().viewers.remove(uuid) && e.getValue().viewers.isEmpty()) {
				it.remove();
				setLit(e.getKey().world(), e.getKey().pos(), e.getValue().manuallyLit);
			}
		}
	}

	/** Chamado quando o bloco deixa de existir: não adianta tentar devolver estado a ele. */
	public static void forget(ServerWorld world, BlockPos pos) {
		OPEN.remove(new Key(world, pos.toImmutable()));
	}

	private static boolean isLit(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.getBlock() instanceof PokebookBlock && state.get(PokebookBlock.LIT);
	}

	private static void setLit(ServerWorld world, BlockPos pos, boolean lit) {
		BlockState state = world.getBlockState(pos);
		// O bloco pode ter sido quebrado ou substituído entre a abertura e o fechamento.
		if (!(state.getBlock() instanceof PokebookBlock) || state.get(PokebookBlock.LIT) == lit) {
			return;
		}
		world.setBlockState(pos, state.with(PokebookBlock.LIT, lit), Block.NOTIFY_ALL);
	}
}
