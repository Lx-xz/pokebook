package io.github.lxxz.pokebook.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.lxxz.pokebook.battle.BattleChallenges;
import io.github.lxxz.pokebook.network.TrackTargetPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

/**
 * {@code /pokebook rastrear <x y z> [dimensão] [rótulo]}: aponta a seta do HUD para um
 * lugar.
 *
 * <p>Existe por causa do chat. O único jeito de um texto clicável <em>fazer</em> alguma
 * coisa no vanilla é rodar um comando, então a localização mandada no chat (ver
 * {@code ChatLocation}) é um texto que roda este. Serve também digitado à mão, para quem
 * recebeu coordenadas de outro jeito.
 *
 * <p>Sem nível de permissão: só mexe na seta de quem roda, que é estado do cliente dele.
 * Não teleporta, não revela nada — as coordenadas são as que ele mesmo escreveu ou clicou.
 */
public final class PokebookCommand {
	/** O rótulo aparece no HUD, numa linha só; o resto de um texto longo seria cortado lá mesmo. */
	private static final int MAX_LABEL_LENGTH = 32;

	private PokebookCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("pokebook")
			// O alvo do texto clicável de um desafio de batalha. Só age sobre um desafio que
			// o servidor registrou para quem roda; digitar à mão não inventa nada.
			.then(CommandManager.literal("batalha")
				.then(CommandManager.literal("aceitar")
					.then(CommandManager.argument("challenger", StringArgumentType.word())
						.executes(context -> battle(context, true))))
				.then(CommandManager.literal("recusar")
					.then(CommandManager.argument("challenger", StringArgumentType.word())
						.executes(context -> battle(context, false)))))
			.then(CommandManager.literal("rastrear")
				.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
					.executes(context -> track(context, false, false))
					.then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
						.executes(context -> track(context, true, false))
						.then(CommandManager.argument("label", StringArgumentType.greedyString())
							.executes(context -> track(context, true, true)))))));
	}

	private static int battle(CommandContext<ServerCommandSource> context, boolean accept) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		String challenger = StringArgumentType.getString(context, "challenger");
		if (accept) {
			BattleChallenges.accept(player, challenger);
		} else {
			BattleChallenges.decline(player, challenger);
		}
		return 1;
	}

	private static int track(CommandContext<ServerCommandSource> context, boolean hasDimension, boolean hasLabel)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			// Do console não há seta para apontar.
			return 0;
		}

		BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
		RegistryKey<World> dimension = hasDimension
			? DimensionArgumentType.getDimensionArgument(context, "dimension").getRegistryKey()
			: player.getServerWorld().getRegistryKey();
		String label = hasLabel
			? StringArgumentType.getString(context, "label")
			: "%d, %d, %d".formatted(pos.getX(), pos.getY(), pos.getZ());
		if (label.length() > MAX_LABEL_LENGTH) {
			label = label.substring(0, MAX_LABEL_LENGTH);
		}

		ServerPlayNetworking.send(player, new TrackTargetPayload(GlobalPos.create(dimension, pos), label));
		return 1;
	}
}
