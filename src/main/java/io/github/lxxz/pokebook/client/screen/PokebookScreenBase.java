package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.network.ClosePokebookPayload;
import io.github.lxxz.pokebook.sound.PokebookSounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
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

	/**
	 * A moldura, deitada ou em pé. Deixou de ser constante quando o poképhone entrou: a
	 * mesma lista de missões precisa caber nas duas proporções, então toda medida de tela
	 * é calculada a partir daqui em vez de ser um número escrito à mão.
	 */
	private static final int LANDSCAPE_WIDTH = 240;
	private static final int LANDSCAPE_HEIGHT = 160;
	// 176 e não 160: com três abas lado a lado, 160 deixava cada uma com 48 px e os
	// rótulos se sobrepunham. Largura de aba tem de caber no idioma mais verboso.
	private static final int PORTRAIT_WIDTH = 176;
	private static final int PORTRAIT_HEIGHT = 220;

	/**
	 * Liga o contorno de depuração: cada componente ganha uma borda de 1 px, para se ver a
	 * disposição real em vez de se deduzir dos números.
	 *
	 * <p>Deixe {@code false} antes de considerar qualquer visual pronto — a borda desenha
	 * por cima de tudo, inclusive do que ela está medindo.
	 */
	public static final boolean LAYOUT_DEBUG = true;

	/** Vermelho: a moldura inteira. */
	protected static final int DEBUG_PANEL = 0xFFFF2040;
	/** Verde: áreas que contêm outras coisas — a lista, a faixa de abas. */
	protected static final int DEBUG_AREA = 0xFF00C060;
	/** Azul: cada linha de uma lista. */
	protected static final int DEBUG_ROW = 0xFF3080FF;
	/** Amarelo: o que responde a clique. */
	protected static final int DEBUG_HIT = 0xFFFFC000;
	/** Roxo: caixas de texto, para se ver a folga real em volta das letras. */
	protected static final int DEBUG_TEXT = 0xFFC060FF;

	// Paleta para fundo CLARO. A textura é ciano claro, então texto claro sumiria nela.
	// Pelo mesmo motivo o texto vai sem sombra: sombra escura sob texto escuro empasta.
	protected static final int COLOR_TEXT = 0xFF102028;
	protected static final int COLOR_ACCENT = 0xFF0D5A70;
	protected static final int COLOR_DONE = 0xFF1B6B2A;
	protected static final int COLOR_MUTED = 0xFF5C7A86;

	protected final PokebookSession session;

	/** Verdadeiro enquanto trocamos para outra tela do próprio pokébook. */
	private boolean keepingSession;

	protected PokebookScreenBase(Text title, PokebookSession session) {
		super(title);
		this.session = session;
	}

	protected int panelWidth() {
		return session.portrait() ? PORTRAIT_WIDTH : LANDSCAPE_WIDTH;
	}

	protected int panelHeight() {
		return session.portrait() ? PORTRAIT_HEIGHT : LANDSCAPE_HEIGHT;
	}

	/**
	 * Margem entre a borda da moldura e o conteúdo.
	 *
	 * <p>A arte tem borda escura e canto arredondado; o azul claro só começa aqui dentro.
	 * Conteúdo encostado na borda fica em cima do contorno e "escorrega" pelo canto
	 * redondo. <b>Toda medida de tela sai de {@link #contentX()} e companhia</b>, nunca de
	 * {@code panelX()} — é o que garante que nada nasça fora da área clara.
	 */
	protected static final int CONTENT_INSET = 10;

	/** Lado dos botões quadrados de canto (voltar e fechar). */
	protected static final int CORNER_BUTTON = 14;

	protected int panelX() {
		return (width - panelWidth()) / 2;
	}

	protected int panelY() {
		return (height - panelHeight()) / 2;
	}

	/**
	 * Se a mão do jogador deve sumir enquanto esta tela estiver aberta.
	 *
	 * <p>Vale só para o aparelho de bolso: ele é segurado na frente do rosto, e o braço
	 * ficaria sobrando na cena. Com o pokébook, a tela é do bloco à frente e a mão não
	 * atrapalha nada.
	 *
	 * <p>Quem consulta isto é o mixin em {@code HeldItemRendererMixin} — o desenho da mão
	 * é do jogo, não nosso.
	 */
	public boolean hidesHands() {
		return session.portable();
	}

	/** Canto superior esquerdo da área clara. */
	protected int contentX() {
		return panelX() + CONTENT_INSET;
	}

	protected int contentY() {
		return panelY() + CONTENT_INSET;
	}

	protected int contentWidth() {
		return panelWidth() - CONTENT_INSET * 2;
	}

	protected int contentHeight() {
		return panelHeight() - CONTENT_INSET * 2;
	}

	/** Primeira linha livre abaixo da barra de título. */
	protected int contentTop() {
		return contentY() + CORNER_BUTTON + 4;
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
		SpriteButtonWidget close = new SpriteButtonWidget(
			contentX() + contentWidth() - CORNER_BUTTON, contentY(), CORNER_BUTTON,
			Identifier.of(Pokebook.MOD_ID, "x"),
			Text.translatable("screen.pokebook.close"), button -> close());
		close.setTooltip(Tooltip.of(Text.translatable("screen.pokebook.close")));
		addDrawableChild(close);

		PokebookScreenBase parent = parentScreen();
		if (parent != null) {
			SpriteButtonWidget back = new SpriteButtonWidget(
				contentX(), contentY(), CORNER_BUTTON,
				Identifier.of(Pokebook.MOD_ID, "seta"),
				Text.translatable("screen.pokebook.back"), button -> navigateTo(parent));
			back.setTooltip(Tooltip.of(Text.translatable("screen.pokebook.back")));
			addDrawableChild(back);
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
		if (client != null && client.player != null) {
			SoundEvent page = Registries.SOUND_EVENT.get(PokebookSounds.PAGE);
			if (page != null) {
				client.player.playSound(page, 0.5f, 1.0f);
			}
		}
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
		// Um aparelho de bolso não tem de onde se afastar.
		session.pos().ifPresent(pos -> {
			if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > MAX_DISTANCE_SQUARED) {
				close();
			}
		});
	}

	/**
	 * A moldura e o título, <b>antes</b> dos widgets.
	 *
	 * <p>Isto vive em {@code renderBackground} e não em {@code render} por uma razão de
	 * ordem: o {@code render} do vanilla desenha o fundo <em>e depois</em> os widgets. A
	 * moldura estava sendo desenhada depois de {@code super.render}, ou seja, <b>por cima
	 * de todos os botões</b>.
	 *
	 * <p>Isso passou despercebido por muito tempo porque botão do vanilla desenha texto e
	 * sprite em camadas que o jogo esvazia mais tarde, então ele reaparecia por cima. Um
	 * widget nosso que pinte o próprio fundo com {@code fill} não tem essa sorte: sumia
	 * inteiro. Foi o que aconteceu com os ícones da tela inicial.
	 */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);

		int x = panelX();
		int y = panelY();

		// A arte é deitada; em pé ela é esticada para a moldura nova. Fica aceitável porque
		// é um retângulo de cor sólida com borda, mas é provisório: o redesenho com sprites
		// e nine-slice resolve isto de verdade, sem deformar canto nenhum.
		// ATENÇÃO à ordem: nesta sobrecarga o TAMANHO vem antes de u,v -- ao contrário da
		// sobrecarga curta, onde u,v vêm antes. Todos os parâmetros são numéricos, então
		// trocar a ordem compila e desenha um retângulo de tamanho zero, sem erro nenhum.
		context.drawTexture(TEXTURE, x, y, panelWidth(), panelHeight(), 0.0f, 0.0f,
			LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

		// Não existe versão centralizada sem sombra, então o x é calculado aqui. O título
		// fica centralizado na área clara, na altura dos botões de canto.
		context.drawText(textRenderer, title, titleX(), titleY(), COLOR_TEXT, false);
	}

	private int titleX() {
		return contentX() + (contentWidth() - textRenderer.getWidth(title)) / 2;
	}

	private int titleY() {
		return contentY() + (CORNER_BUTTON - textRenderer.fontHeight) / 2 + 1;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Desenha o fundo (moldura e título) e depois os widgets, nesta ordem.
		super.render(context, mouseX, mouseY, delta);

		// O conteúdo próprio da tela vem por último, sobre os widgets. Nas telas atuais os
		// dois não se sobrepõem: as abas ficam acima da lista, e a lista é desenhada à mão.
		renderPanel(context, mouseX, mouseY, delta);

		if (LAYOUT_DEBUG) {
			outline(context, panelX(), panelY(), panelWidth(), panelHeight(), DEBUG_PANEL);
			outline(context, contentX(), contentY(), contentWidth(), contentHeight(), DEBUG_AREA);
			outlineText(context, title, titleX(), titleY(), DEBUG_TEXT);
			// Os botões se desenham sozinhos; aqui só marcamos onde eles de fato estão.
			for (net.minecraft.client.gui.Element child : children()) {
				if (child instanceof net.minecraft.client.gui.widget.ClickableWidget widget) {
					outline(context, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), DEBUG_HIT);
				}
			}
		}
	}

	/**
	 * Contorno de 1 px, se a depuração estiver ligada. Não faz nada quando desligada, para
	 * a chamada poder ficar no código de desenho sem um {@code if} em volta de cada uma.
	 */
	protected void outline(DrawContext context, int x, int y, int width, int height, int color) {
		if (!LAYOUT_DEBUG) {
			return;
		}
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}

	/** O mesmo, em volta de um texto já desenhado. */
	protected void outlineText(DrawContext context, Text text, int x, int y, int color) {
		outline(context, x - 1, y - 1, textRenderer.getWidth(text) + 2, textRenderer.fontHeight + 2, color);
	}

	/** Conteúdo próprio de cada tela, desenhado dentro da moldura. */
	protected abstract void renderPanel(DrawContext context, int mouseX, int mouseY, float delta);

	@Override
	public void removed() {
		// Só o bloco precisa saber que fechamos: é a tela dele que apaga.
		if (!keepingSession && client != null && client.getNetworkHandler() != null) {
			session.pos().ifPresent(pos -> ClientPlayNetworking.send(new ClosePokebookPayload(pos)));
		}
		super.removed();
	}
}
