package io.github.lxxz.pokebook.phone;

import com.mojang.serialization.Codec;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.config.ServerConfig;
import io.github.lxxz.pokebook.network.FlashlightPayload;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A lanterna do poképhone: uma luz que anda com o jogador.
 *
 * <p><b>Feita com o bloco de luz do vanilla</b> ({@code minecraft:light}), que é invisível,
 * não tem colisão e emite o nível que se pedir. A cada passo a luz é posta no bloco da cabeça
 * e tirada do anterior. É o caminho que não exige mod de luz dinâmica — e por isso é do
 * <b>servidor</b>: a luz é de verdade, os outros veem, e ela impede monstro de nascer ali,
 * como uma tocha na mão faria.
 *
 * <p><b>Só em ar.</b> A luz nunca substitui bloco nenhum — nem água, nem grama alta. Onde não
 * há ar na cabeça nem nos pés, a lanterna fica sem luz até sair dali. É a regra que garante
 * que apagar nunca destrói nada: só se tira um bloco de luz, e só um que nós pusemos.
 *
 * <p><b>O que foi posto fica anotado no mundo.</b> Um servidor que cai com lanternas acesas
 * deixaria blocos de luz invisíveis espalhados para sempre. A lista vai para um anexo do mundo
 * principal, e ao ligar o servidor tudo o que ficou nela é apagado.
 *
 * <p>Apaga sozinha quando o jogador deixa de carregar o poképhone, morre, muda de dimensão,
 * sai do servidor, ou quando o dono do servidor desliga a lanterna no config.
 */
public final class Flashlight {
	/** Forte como uma tocha na mão (14), um degrau abaixo para não competir com ela. */
	private static final int LEVEL = 13;
	/** De quantos em quantos ticks a luz acompanha. Dois já parece contínuo andando. */
	private static final int INTERVAL = 2;

	private record Placed(List<GlobalPos> lights) {
		static final Codec<Placed> CODEC = GlobalPos.CODEC.listOf().xmap(Placed::new, Placed::lights);

		Placed {
			lights = List.copyOf(lights);
		}
	}

	private static final AttachmentType<Placed> PLACED = AttachmentRegistry.create(
		Identifier.of(Pokebook.MOD_ID, "flashlight_lights"),
		builder -> builder
			.initializer(() -> new Placed(List.of()))
			.persistent(Placed.CODEC)
	);

	/** Quem está com a lanterna acesa, e onde está a luz dele agora (nula: sem lugar). */
	private static final Map<UUID, GlobalPos> LIT = new HashMap<>();
	/** Quem pediu a lanterna acesa. Separado de LIT: acesa sem luz é "dentro d'água". */
	private static final Set<UUID> ON = new HashSet<>();

	private Flashlight() {
	}

	public static void register() {
	}

	/** O pedido do cliente. */
	public static void set(ServerPlayerEntity player, boolean on) {
		if (on && (!ServerConfig.features().flashlight() || !Pokephones.carries(player))) {
			on = false;
		}
		if (on) {
			ON.add(player.getUuid());
			move(player);
		} else {
			off(player);
		}
		ServerPlayNetworking.send(player, new FlashlightPayload(on));
	}

	public static void tick(MinecraftServer server) {
		if (ON.isEmpty() || server.getTicks() % INTERVAL != 0) {
			return;
		}
		boolean enabled = ServerConfig.features().flashlight();
		for (UUID uuid : List.copyOf(ON)) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
			if (player == null) {
				continue;
			}
			if (!enabled || !player.isAlive() || player.isSpectator() || !Pokephones.carries(player)) {
				off(player);
				ServerPlayNetworking.send(player, new FlashlightPayload(false));
			} else {
				move(player);
			}
		}
	}

	/** Leva a luz para onde o jogador está agora. */
	private static void move(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		GlobalPos target = spot(world, player);
		GlobalPos current = LIT.get(player.getUuid());
		if (target != null && target.equals(current)) {
			return;
		}
		LIT.remove(player.getUuid());
		if (current != null) {
			remove(player.server, current);
		}
		if (target != null) {
			world.setBlockState(target.pos(), Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, LEVEL), Block.NOTIFY_ALL);
			remember(player.server, target, true);
		}
		LIT.put(player.getUuid(), target);
	}

	/** Cabeça, senão pés; só se for ar. */
	private static GlobalPos spot(ServerWorld world, ServerPlayerEntity player) {
		BlockPos head = BlockPos.ofFloored(player.getEyePos());
		for (BlockPos pos : new BlockPos[] {head, player.getBlockPos()}) {
			BlockState state = world.getBlockState(pos);
			GlobalPos global = GlobalPos.create(world.getRegistryKey(), pos);
			// Luz de outra lanterna no mesmo lugar: aproveita, sem se apossar dela.
			if (state.isAir() || (state.isOf(Blocks.LIGHT) && isOurs(global))) {
				return global;
			}
		}
		return null;
	}

	private static boolean isOurs(GlobalPos pos) {
		return LIT.containsValue(pos);
	}

	private static void off(ServerPlayerEntity player) {
		ON.remove(player.getUuid());
		GlobalPos current = LIT.remove(player.getUuid());
		if (current != null) {
			remove(player.server, current);
		}
	}

	/**
	 * Tira a luz, se ninguém mais estiver usando aquele lugar e se ela ainda estiver lá.
	 * Quem chama já saiu de {@code LIT}: o que sobrar lá neste lugar é outra lanterna junto.
	 */
	private static void remove(MinecraftServer server, GlobalPos pos) {
		if (LIT.containsValue(pos)) {
			return;
		}
		ServerWorld world = server.getWorld(pos.dimension());
		if (world != null && world.getBlockState(pos.pos()).isOf(Blocks.LIGHT)) {
			world.setBlockState(pos.pos(), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
		}
		remember(server, pos, false);
	}

	private static void remember(MinecraftServer server, GlobalPos pos, boolean add) {
		ServerWorld overworld = server.getOverworld();
		List<GlobalPos> lights = new ArrayList<>(overworld.getAttachedOrCreate(PLACED).lights());
		boolean changed = add ? !lights.contains(pos) && lights.add(pos) : lights.remove(pos);
		if (changed) {
			overworld.setAttached(PLACED, new Placed(lights));
		}
	}

	public static void disconnect(ServerPlayerEntity player) {
		off(player);
	}

	/** Mudou de dimensão ou renasceu: a luz velha ficou para trás. */
	public static void relocated(ServerPlayerEntity player) {
		GlobalPos current = LIT.remove(player.getUuid());
		if (current != null) {
			remove(player.server, current);
		}
	}

	/** Ao ligar o servidor: apaga o que uma queda deixou aceso. */
	public static void cleanUp(MinecraftServer server) {
		ServerWorld overworld = server.getOverworld();
		List<GlobalPos> leftovers = overworld.getAttachedOrCreate(PLACED).lights();
		for (GlobalPos pos : leftovers) {
			ServerWorld world = server.getWorld(pos.dimension());
			if (world != null && world.getBlockState(pos.pos()).isOf(Blocks.LIGHT)) {
				world.setBlockState(pos.pos(), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
		if (!leftovers.isEmpty()) {
			Pokebook.LOGGER.info("[Pokébook] {} luz(es) de lanterna deixadas por um desligamento foram apagadas.", leftovers.size());
			overworld.setAttached(PLACED, new Placed(List.of()));
		}
	}

	/** Ao desligar o servidor: apaga todas, para não depender da limpeza acima. */
	public static void shutdown(MinecraftServer server) {
		for (UUID uuid : List.copyOf(ON)) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
			if (player != null) {
				off(player);
			}
		}
		ON.clear();
		LIT.clear();
	}
}
