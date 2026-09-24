package io.github.lxxz.pokebook.client.render;

import io.github.lxxz.pokebook.Pokebook;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Faz a tela acesa brilhar de verdade no escuro.
 *
 * <p>O problema: a luz do bloco ilumina o <em>ambiente</em>, mas a textura da própria tela
 * continua sendo sombreada pela iluminação do lugar — de noite ela escurecia junto com o
 * resto, o oposto do que uma tela ligada faz.
 *
 * <p>A solução é a API de renderização do Fabric. Cada face do modelo é reemitida com um
 * material próprio, e as faces cujo desenho é uma das texturas de tela acesa ganham
 * {@code emissive}, que as desenha em brilho máximo independentemente da luz do bloco.
 * {@code disableDiffuse} evita que a inclinação da tampa deixe a tela mais escura de um
 * lado, e o oclusão de ambiente é desligado pelo mesmo motivo.
 *
 * <p>Esta versão do Fabric API <b>não</b> tem os "material maps" (aquele JSON em
 * {@code assets/<ns>/materialmaps/}), que seriam o caminho sem código. Confirmado com
 * {@code javap}: não existe nenhuma classe de mapa de material no jar. Por isso o wrapper.
 *
 * <p>A seleção é por <b>sprite</b>, não por modelo: um único wrapper serve aos quatro
 * modelos de tela, e o modelo apagado passa por ele sem nenhuma face emissiva — não há
 * regra por estado a manter em dia. Acrescentar um nível de tela novo é acrescentar a
 * textura ao conjunto abaixo.
 */
public class EmissiveScreenModel extends ForwardingBakedModel {
	/**
	 * As texturas que representam tela ligada. A apagada fica de fora de propósito.
	 *
	 * <p>Era um conjunto de três enquanto a tela tinha quatro níveis de brilho. Com a v3 a
	 * tela voltou a ser binária — {@code screen_on} e {@code screen_off} —, e os nomes dos
	 * níveis intermediários foram removidos daqui porque as texturas deixaram de existir.
	 * Continuar listando-os não quebrava nada (nenhum sprite casaria), mas mentia sobre o
	 * que o mod tem.
	 */
	private static final Set<Identifier> LIT_SPRITES = Set.of(
		Identifier.of(Pokebook.MOD_ID, "block/screen_on")
	);

	/**
	 * As faces a percorrer. O {@code null} no fim não é descuido: no formato de modelo do
	 * Minecraft, {@code null} é o balde das faces que não encostam em nenhuma parede do
	 * cubo e portanto nunca são cortadas. A tela inclinada do pokébook está toda aí.
	 */
	private static final Direction[] FACES =
		Arrays.copyOf(Direction.values(), Direction.values().length + 1);

	private final RenderMaterial standard;
	private final RenderMaterial emissive;

	public EmissiveScreenModel(BakedModel wrapped) {
		this.wrapped = wrapped;

		Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		this.standard = renderer.materialFinder().clear().find();
		this.emissive = renderer.materialFinder().clear()
			.emissive(true)
			.disableDiffuse(true)
			.ambientOcclusion(TriState.FALSE)
			.find();
	}

	/** Sem isto o jogo usa o caminho rápido do vanilla e {@link #emitBlockQuads} nunca roda. */
	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView world, BlockState state, BlockPos pos,
	                           Supplier<Random> randomSupplier, RenderContext context) {
		QuadEmitter emitter = context.getEmitter();

		for (Direction face : FACES) {
			for (BakedQuad quad : wrapped.getQuads(state, face, randomSupplier.get())) {
				emitter.fromVanilla(quad, isLitScreen(quad) ? emissive : standard, face);
				emitter.emit();
			}
		}
	}

	private static boolean isLitScreen(BakedQuad quad) {
		return LIT_SPRITES.contains(quad.getSprite().getContents().getId());
	}
}
