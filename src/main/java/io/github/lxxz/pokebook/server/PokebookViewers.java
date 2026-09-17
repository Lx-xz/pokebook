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
 * <p>A tela é por jogador, mas a luz é estado de bloco — e estado de bloco é
 * compartilhado. Sem contagem, o primeiro jogador a fechar apagaria a tela na cara de
 * quem ainda estivesse olhando. Aqui a luz só apaga quando o <em>último</em> espectador
 * sai.
 *
 * <p>Isto vive só em memória e de propósito. Se o servidor cair com a luz acesa, ela
 * volta acesa e sem espectadores — o shift+clique resolve manualmente. Persistir esse
 * estado custaria muito mais do que o problema vale.
 *
 * <p>Tudo aqui roda na thread do servidor, então as coleções não precisam ser sincronizadas.
 */
public final class PokebookViewers {
	/** Posição sozinha não identifica um bloco: as mesmas coordenadas existem em cada dimensão. */
	private record Key(ServerWorld world, BlockPos pos) {
	}

	private static final Map<Key, Set<UUID>> VIEWERS = new HashMap<>();

	private PokebookViewers() {
	}

	public static void open(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
		Key key = new Key(world, pos.toImmutable());
		VIEWERS.computeIfAbsent(key, k -> new HashSet<>()).add(player.getUuid());
		setLit(world, pos, true);
	}

	public static void close(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
		Key key = new Key(world, pos.toImmutable());
		Set<UUID> viewers = VIEWERS.get(key);
		if (viewers == null) {
			return;
		}
		viewers.remove(player.getUuid());
		if (viewers.isEmpty()) {
			VIEWERS.remove(key);
			setLit(world, pos, false);
		}
	}

	/** Chamado quando o jogador desconecta: ele não vai mandar o pacote de fechamento. */
	public static void removePlayer(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		Iterator<Map.Entry<Key, Set<UUID>>> it = VIEWERS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Key, Set<UUID>> entry = it.next();
			if (entry.getValue().remove(uuid) && entry.getValue().isEmpty()) {
				it.remove();
				setLit(entry.getKey().world(), entry.getKey().pos(), false);
			}
		}
	}

	/** Chamado quando o bloco deixa de existir: não adianta tentar apagar a luz dele. */
	public static void forget(ServerWorld world, BlockPos pos) {
		VIEWERS.remove(new Key(world, pos.toImmutable()));
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
