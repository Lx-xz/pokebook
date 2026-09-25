package io.github.lxxz.pokebook.client.map;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * O mapa da área em volta do pokébook, desenhado com o que o cliente já sabe.
 *
 * <p><b>O "próxima" é o que torna isto viável.</b> O cliente só tem os chunks ao alcance
 * da distância de visão; é exatamente essa área que o mapa mostra. Não há pacote novo, nada
 * é pedido ao servidor, e não há como revelar o que o jogador não poderia ver andando.
 *
 * <p><b>As cores são as do mapa do vanilla</b> ({@code MapColor}), com o mesmo sombreado: um
 * bloco mais alto que o vizinho ao norte sai mais claro, um mais baixo sai mais escuro. A água
 * escurece com a profundidade. Quem conhece o item mapa reconhece na hora.
 *
 * <p><b>Gerado aos poucos, e guardado.</b> São {@value #SIZE}² colunas, e cada uma desce do
 * topo até achar um bloco com cor. Tudo num quadro só engasgaria; aqui vão
 * {@value #ROWS_PER_FRAME} linhas por quadro, e o mapa aparece de cima para baixo em
 * poucos décimos de segundo. Reabrir no mesmo lugar logo depois reaproveita a imagem.
 *
 * <p>A imagem é uma textura só, de {@value #SIZE}×{@value #SIZE}, do tamanho de um mapa do
 * vanilla. Vive até sair do servidor.
 */
public final class AreaMap {
	/** Lado do mapa, em blocos — um pixel por bloco. */
	public static final int SIZE = 128;
	public static final Identifier TEXTURE = Identifier.of(Pokebook.MOD_ID, "map/area");

	private static final int ROWS_PER_FRAME = 8;
	/** Reabrir a menos disto do centro antigo, e há pouco tempo, reaproveita o mapa. */
	private static final int REUSE_DISTANCE = 8;
	private static final long REUSE_MS = 30_000;
	/** Chunk que o cliente não tem: cinza-escuro, para ler como "fora do alcance". ABGR. */
	private static final int UNKNOWN = 0xFF2A2A2A;
	private static final int MAX_WATER_DEPTH = 10;

	private static NativeImageBackedTexture texture;
	private static RegistryKey<World> dimension;
	private static int originX;
	private static int originZ;
	private static int nextRow = SIZE;
	private static long finishedAt;

	private AreaMap() {
	}

	/** Começa (ou reaproveita) o mapa em volta de {@code center}. */
	public static void begin(MinecraftClient client, BlockPos center) {
		ClientWorld world = client.world;
		if (world == null) {
			return;
		}
		int x = center.getX() - SIZE / 2;
		int z = center.getZ() - SIZE / 2;
		boolean reuse = texture != null && nextRow >= SIZE
			&& world.getRegistryKey().equals(dimension)
			&& Math.abs(x - originX) <= REUSE_DISTANCE && Math.abs(z - originZ) <= REUSE_DISTANCE
			&& System.currentTimeMillis() - finishedAt < REUSE_MS;
		if (reuse) {
			return;
		}
		if (texture == null) {
			// A textura passa a ser dona da imagem; destruí-la fecha a memória nativa.
			texture = new NativeImageBackedTexture(new NativeImage(SIZE, SIZE, false));
			client.getTextureManager().registerTexture(TEXTURE, texture);
		}
		NativeImage image = texture.getImage();
		if (image != null) {
			image.fillRect(0, 0, SIZE, SIZE, UNKNOWN);
			texture.upload();
		}
		dimension = world.getRegistryKey();
		originX = x;
		originZ = z;
		nextRow = 0;
	}

	/** Anda mais algumas linhas. Chamado a cada quadro com a tela aberta. */
	public static void step(MinecraftClient client) {
		ClientWorld world = client.world;
		if (texture == null || nextRow >= SIZE || world == null || !world.getRegistryKey().equals(dimension)) {
			return;
		}
		NativeImage image = texture.getImage();
		if (image == null) {
			return;
		}
		BlockPos.Mutable pos = new BlockPos.Mutable();
		int end = Math.min(SIZE, nextRow + ROWS_PER_FRAME);
		for (int row = nextRow; row < end; row++) {
			int z = originZ + row;
			for (int column = 0; column < SIZE; column++) {
				image.setColor(column, row, color(world, originX + column, z, pos));
			}
		}
		nextRow = end;
		texture.upload();
		if (nextRow >= SIZE) {
			finishedAt = System.currentTimeMillis();
		}
	}

	/** Se o mapa já terminou de ser desenhado. */
	public static boolean ready() {
		return texture != null && nextRow >= SIZE;
	}

	public static int originX() {
		return originX;
	}

	public static int originZ() {
		return originZ;
	}

	/** Esquece o mapa. Um servidor novo, ou o mesmo em outra sessão, desenha do zero. */
	public static void clear(MinecraftClient client) {
		if (texture != null) {
			client.getTextureManager().destroyTexture(TEXTURE);
			texture = null;
		}
		dimension = null;
		nextRow = SIZE;
	}

	// ------------------------------------------------------------------ uma coluna

	/** A cor de uma coluna, em ABGR — o formato do {@code NativeImage} na 1.21.1. */
	private static int color(ClientWorld world, int x, int z, BlockPos.Mutable pos) {
		if (!world.isChunkLoaded(x >> 4, z >> 4)) {
			return UNKNOWN;
		}
		int y = surface(world, x, z, pos);
		if (y == Integer.MIN_VALUE) {
			return UNKNOWN;
		}
		BlockState state = world.getBlockState(pos);
		MapColor mapColor = state.getMapColor(world, pos);

		MapColor.Brightness brightness;
		if (!state.getFluidState().isEmpty()) {
			// Água: quanto mais funda, mais escura. É o que dá para ver o raso da praia.
			int depth = 0;
			BlockPos.Mutable below = pos.mutableCopy();
			while (depth < MAX_WATER_DEPTH && below.getY() > world.getBottomY()
					&& !world.getBlockState(below.move(Direction.DOWN)).getFluidState().isEmpty()) {
				depth++;
			}
			brightness = depth < 2 ? MapColor.Brightness.HIGH
				: depth < 5 ? MapColor.Brightness.NORMAL
				: MapColor.Brightness.LOW;
		} else {
			// Relevo: compara com o vizinho ao norte, como o mapa do vanilla.
			int north = world.isChunkLoaded(x >> 4, (z - 1) >> 4) ? surface(world, x, z - 1, new BlockPos.Mutable()) : y;
			brightness = north == Integer.MIN_VALUE || y == north ? MapColor.Brightness.NORMAL
				: y > north ? MapColor.Brightness.HIGH
				: MapColor.Brightness.LOW;
		}
		return mapColor.getRenderColor(brightness);
	}

	/**
	 * A altura do primeiro bloco com cor, de cima para baixo, deixando {@code pos} nele.
	 * Vidro, ar e o que mais for {@code CLEAR} são atravessados, como no mapa do vanilla.
	 */
	private static int surface(ClientWorld world, int x, int z, BlockPos.Mutable pos) {
		int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
		pos.set(x, top, z);
		while (pos.getY() >= world.getBottomY()) {
			if (world.getBlockState(pos).getMapColor(world, pos) != MapColor.CLEAR) {
				return pos.getY();
			}
			pos.move(Direction.DOWN);
		}
		return Integer.MIN_VALUE;
	}
}
