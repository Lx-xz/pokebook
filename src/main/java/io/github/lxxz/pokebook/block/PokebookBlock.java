package io.github.lxxz.pokebook.block;

import io.github.lxxz.pokebook.mission.MissionService;
import io.github.lxxz.pokebook.network.OpenPokebookPayload;
import io.github.lxxz.pokebook.server.PokebookViewers;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Optional;

public class PokebookBlock extends Block {
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

	/** Brilho da tela: 0 apagada, 3 acesa. Os intermediários existem para a transição. */
	public static final int SCREEN_OFF = 0;
	public static final int SCREEN_ON = 3;
	public static final IntProperty SCREEN = IntProperty.of("screen", SCREEN_OFF, SCREEN_ON);

	/**
	 * Luz emitida por nível.
	 *
	 * <p>Binária enquanto a animação está desligada: a v3 tem só duas artes de tela, e os
	 * níveis intermediários mostram a apagada. Emitir luz 2 ou 5 com a tela visivelmente
	 * escura seria incoerente. Quando as quatro artes existirem, volta a ser
	 * {@code { 0, 2, 5, 7 }} — é a única linha a mexer.
	 */
	private static final int[] LIGHT_BY_LEVEL = { 0, 0, 0, 7 };

	public static int lightFor(int level) {
		return LIGHT_BY_LEVEL[level];
	}

	// Caixas do modelo, em pixels (0..16), para o bloco virado ao norte.
	//
	// VoxelShape só conhece caixas alinhadas aos eixos, e a v3 não tem rotação nenhuma:
	// a tampa é vertical e a base é plana, então duas caixas bastam.
	//
	// Envelope do modelo: x 1..15, y 0..12, z 2..14. As duas dobradiças cruzam z=12 e
	// não cabem inteiras em nenhuma das caixas — mas a UNIÃO as cobre, que é o que vale.
	// Verificado por amostragem, não no olho. Se o modelo mudar, recalcule.
	private static final double[][] BOXES = {
		// base: chassi + teclado + touchpad
		{ 1, 0,   2,  15, 2.75, 12 },
		// tampa vertical: bisel, painel de trás e o resto das dobradiças
		{ 1, 1.5, 12, 15, 12,   14 },
	};

	// A forma não muda em tempo de execução: só depende do FACING. Montar a união a
	// cada chamada seria desperdício, porque getOutlineShape roda a cada raycast de
	// mira. Aqui ela é pré-calculada uma vez por direção, indexada por quartos de volta.
	private static final VoxelShape[] SHAPES = buildShapes();

	private static VoxelShape[] buildShapes() {
		VoxelShape[] shapes = new VoxelShape[4];
		for (int turns = 0; turns < 4; turns++) {
			VoxelShape shape = VoxelShapes.empty();
			for (double[] box : BOXES) {
				shape = VoxelShapes.union(shape, rotate(box, turns));
			}
			shapes[turns] = shape.simplify();
		}
		return shapes;
	}

	public PokebookBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(SCREEN, SCREEN_OFF));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, SCREEN);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		// Nada de cliente aqui: a tela é aberta pelo pacote, no entrypoint de cliente.
		// Mencionar classes de net.minecraft.client neste arquivo derrubaria o servidor
		// dedicado com NoClassDefFoundError — e um "if (world.isClient)" não protegeria,
		// porque a referência é resolvida antes do desvio ser avaliado.
		if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.SUCCESS;
		}

		if (player.isSneaking()) {
			// Shift+clique é o "sempre aceso": vale por si, e fechar a interface não o desfaz.
			PokebookViewers.toggleManual(serverWorld, pos, state);
			return ActionResult.SUCCESS;
		}

		PokebookViewers.open(serverWorld, pos, serverPlayer);
		ServerPlayNetworking.send(serverPlayer, new OpenPokebookPayload(
			Optional.of(pos), serverPlayer.getName().getString(), MissionService.snapshot(serverPlayer)));
		return ActionResult.SUCCESS;
	}

	/**
	 * Um passo da transição da tela.
	 *
	 * <p>Tick agendado de bloco, e não BlockEntity: é barato, é salvo junto com o chunk e
	 * sobrevive a recarregar o mundo. Cada tick anda um nível e agenda o próximo, o que
	 * transforma um mecanismo de gatilho único numa animação encadeada.
	 */
	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		PokebookViewers.stepScreen(world, pos);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		// Só quando o bloco deixa de ser um pokébook — alternar LIT também passa por aqui.
		if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
			PokebookViewers.forget(serverWorld, pos);
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[quarterTurns(state.get(FACING))];
	}

	private static int quarterTurns(Direction facing) {
		return switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	// Gira a caixa no eixo Y, no mesmo sentido horário que o "y" do blockstate.
	private static VoxelShape rotate(double[] box, int turns) {
		double minX = box[0], minZ = box[2], maxX = box[3], maxZ = box[5];
		for (int i = 0; i < turns; i++) {
			double newMinX = 16 - maxZ, newMaxX = 16 - minZ;
			double newMinZ = minX, newMaxZ = maxX;
			minX = newMinX; maxX = newMaxX; minZ = newMinZ; maxZ = newMaxZ;
		}
		return Block.createCuboidShape(minX, box[1], minZ, maxX, box[4], maxZ);
	}
}
