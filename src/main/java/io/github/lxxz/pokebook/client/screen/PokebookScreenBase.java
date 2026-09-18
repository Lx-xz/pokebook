package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.network.ClosePokebookPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * O que toda tela do pokébook compartilha: a moldura, o fechamento por distância e o
 * aviso ao servidor.
 *
 * <p><b>A armadilha que isto resolve:</b> {@code removed()} dispara em qualquer troca de
 * tela, inclusive ao navegar do menu para as missões. Sem cuidado, sair do menu mandaria
 * o pacote de fechamento e apagaria a luz do bloco no meio da navegação. Por isso a
 * troca entre telas do pokébook passa por {@link #navigateTo}, que marca a sessão como
 * contínua. Só a saída de verdade avisa o servidor.
 */
public abstract class PokebookScreenBase extends Screen {
	/** Mesma folga que baú e fornalha usam (8 blocos). */
	private static final double MAX_DISTANCE_SQUARED = 64.0;

	/** A arte ocupa 240x160 no canto superior esquerdo de um arquivo 256x256. */
	private static final Identifier TEXTURE =
		Identifier.of(Pokebook.MOD_ID, "textures/gui/pokebook_gui.png");
	private static final int TEXTURE_SIZE = 256;

	protected static final int PANEL_WIDTH = 240;
	protected static final int PANEL_HEIGHT = 160;

	// Paleta para fundo CLARO. A textura é ciano claro, então texto claro sumiria nela.
	// Pelo mesmo motivo o texto vai sem sombra: sombra escura sob texto escuro empasta.
	protected static final int COLOR_TEXT = 0xFF102028;
	protected static final int COLOR_ACCENT = 0xFF0D5A70;
	protected static final int COLOR_DONE = 0xFF1B6B2A;
	protected static final int COLOR_MUTED = 0xFF5C7A86;

	protected final BlockPos pos;
	protected final String nick;

	/** Verdadeiro enquanto trocamos para outra tela do próprio pokébook. */
	private boolean keepingSession;

	protected PokebookScreenBase(Text title, BlockPos pos, String nick) {
		super(title);
		this.pos = pos;
		this.nick = nick;
	}

	/** Lado dos botões quadrados de canto (voltar e fechar). */
	protected static final int CORNER_BUTTON = 14;

	/** Distância dos botões de canto até a borda da moldura. */
	private static final int CORNER_INSET = 6;

	protected int panelX() {
		return (width - PANEL_WIDTH) / 2;
	}

	protected int panelY() {
		return (height - PANEL_HEIGHT) / 2;
	}

	/**
	 * A barra de título é igual em toda tela: fechar sempre no canto superior direito, e
	 * voltar no superior esquerdo quando houver para onde voltar.
	 *
	 * <p>Fica aqui e não em cada tela porque "onde fica o X" é decisão de uma vez só. Uma
	 * tela nova nasce com os dois no lugar certo sem fazer nada.
	 */
	@Override
	protected final void init() {
		addDrawableChild(ButtonWidget.builder(Text.literal("✕"), button -> close())
			.dimensions(panelX() + PANEL_WIDTH - CORNER_INSET - CORNER_BUTTON, panelY() + CORNER_INSET,
				CORNER_BUTTON, CORNER_BUTTON)
			.tooltip(Tooltip.of(Text.translatable("screen.pokebook.close")))
			.build());

		PokebookScreenBase parent = parentScreen();
		if (parent != null) {
			addDrawableChild(ButtonWidget.builder(Text.literal("←"), button -> navigateTo(parent))
				.dimensions(panelX() + CORNER_INSET, panelY() + CORNER_INSET, CORNER_BUTTON, CORNER_BUTTON)
				.tooltip(Tooltip.of(Text.translatable("screen.pokebook.back")))
				.build());
		}

		initPanel();
	}

	/**
	 * Para onde o botão de voltar leva, ou {@code null} numa tela raiz.
	 *
	 * <p>Devolve uma tela nova a cada chamada de propósito: a de destino precisa ser
	 * construída com os dados do momento do clique, não com os de quando esta abriu.
	 */
	protected PokebookScreenBase parentScreen() {
		return null;
	}

	/** Widgets próprios de cada tela. O equivalente ao {@code init()} do vanilla. */
	protected void initPanel() {
	}

	/** Troca para outra tela do pokébook sem encerrar a sessão com o bloco. */
	protected void navigateTo(PokebookScreenBase next) {
		keepingSession = true;
		if (client != null) {
			client.setScreen(next);
		}
	}

	@Override
	public boolean shouldPause() {
		// Um notebook é usado dentro do mundo, não fora dele.
		return false;
	}

	@Override
	public void tick() {
		// Sem ScreenHandler, ninguém fecha a tela por nós quando o jogador se afasta.
		if (client == null || client.player == null) {
			return;
		}
		if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > MAX_DISTANCE_SQUARED) {
			close();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x = panelX();
		int y = panelY();
		context.drawTexture(TEXTURE, x, y, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

		// Não existe versão centralizada sem sombra, então o x é calculado aqui. O título
		// fica na altura dos botões de canto, entre os dois.
		int titleX = x + (PANEL_WIDTH - textRenderer.getWidth(title)) / 2;
		context.drawText(textRenderer, title, titleX, y + CORNER_INSET + 3, COLOR_TEXT, false);

		renderPanel(context, mouseX, mouseY, delta);
	}

	/** Conteúdo próprio de cada tela, desenhado dentro da moldura. */
	protected abstract void renderPanel(DrawContext context, int mouseX, int mouseY, float delta);

	@Override
	public void removed() {
		if (!keepingSession && client != null && client.getNetworkHandler() != null) {
			ClientPlayNetworking.send(new ClosePokebookPayload(pos));
		}
		super.removed();
	}
}
