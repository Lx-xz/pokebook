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
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class PokebookBlock extends Block {
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = Properties.LIT;

	// Caixas do modelo, em pixels (0..16), para o bloco virado ao norte.
	//
	// VoxelShape só conhece caixas alinhadas aos eixos — não existe caixa rotacionada
	// no Minecraft. Os 22.5° da tampa existem apenas no modelo visual, então aqui ela
	// é aproximada por uma escada de quatro degraus que acompanha a inclinação.
	//
	// Valores derivados da geometria real do modelo: o painel da tampa, depois de
	// rotacionado, ocupa y 0.617..10.239 e z 11.0..15.751 (a tela fica dentro desse
	// envelope). Se o modelo mudar, recalcule — não ajuste no olho.
	private static final double[][] BOXES = {
		// base: chassi + teclado + touchpad
		{ 0, 0,      1,       16, 1.25,  11      },
		// tampa inclinada, de baixo para cima
		{ 0, 0.625,  11,      16, 3,     12.9375 },
		{ 0, 3,      11.625,  16, 5.4375, 13.9375 },
		{ 0, 5.4375, 12.625,  16, 7.8125, 14.9375 },
		{ 0, 7.8125, 13.625,  16, 10.25, 15.75   },
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
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(LIT, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
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
			pos, serverPlayer.getName().getString(), MissionService.snapshot(serverPlayer)));
		return ActionResult.SUCCESS;
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
