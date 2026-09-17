package io.github.lxxz.pokebook.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
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

	// Caixas de colisão do modelo, em pixels (0..16), para o bloco virado ao norte:
	// a base do notebook e a tela inclinada. As outras três direções saem daqui por rotação.
	private static final double[][] BOXES = {
		{ 0, 0, 3, 16, 1, 13 },
		{ 0, 1, 12, 16, 10, 16 },
	};

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
		if (!world.isClient) {
			world.setBlockState(pos, state.cycle(LIT), Block.NOTIFY_ALL);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		VoxelShape shape = VoxelShapes.empty();
		for (double[] box : BOXES) {
			shape = VoxelShapes.union(shape, rotate(box, quarterTurns(state.get(FACING))));
		}
		return shape;
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
