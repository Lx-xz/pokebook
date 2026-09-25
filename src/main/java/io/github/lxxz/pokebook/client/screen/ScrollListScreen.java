package io.github.lxxz.pokebook.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma tela que é uma lista rolável, com uma fileira opcional de botões embaixo.
 *
 * <p>Existe porque seis apps têm essa mesma forma, e cada um repetiria a mesma conta:
 * recorte, deslocamento da rolagem, qual linha está sob o mouse, qual foi clicada. Errar
 * essa conta uma vez é clicar numa linha e abrir a de baixo; errar em seis lugares é
 * procurar o erro seis vezes.
 *
 * <p><b>As linhas são desenhadas à mão, e os botões de baixo são widgets.</b> É a regra do
 * {@code CLAUDE.md}: widget tem posição fixa e a lista muda de posição a cada quadro, então
 * o que rola se desenha e o que não rola é widget.
 *
 * <p>Cada tela diz o que é uma linha ({@link #rows()}), como ela se desenha
 * ({@link #renderRow}) e o que acontece ao clicar ({@link #onRowClicked}). O resto é daqui.
 *
 * @param <T> o tipo de uma linha
 */
public abstract class ScrollListScreen<T> extends PokebookScreenBase {
	protected static final int ROW_HEIGHT = 20;
	protected static final int BUTTON_HEIGHT = 20;
	protected static final int BUTTON_GAP = 4;

	/** Realce da linha sob o mouse — numa lista sem botão, é o que diz que ela é clicável. */
	private static final int HOVER = 0x30000000;

	private int scroll;

	/**
	 * Onde está o mouse neste quadro. Guardado para {@link #renderRow} saber sobre qual
	 * pedaço da linha ele está, sem que cada tela precise receber mais dois parâmetros.
	 */
	protected int mouseX;
	protected int mouseY;

	/** Dica a desenhar no fim do quadro, por cima de tudo. Ver {@link #tooltip}. */
	private Text pendingTooltip;

	protected ScrollListScreen(Text title, PokebookSession session) {
		super(title, session);
	}

	/**
	 * As linhas, na ordem em que aparecem.
	 *
	 * <p>Chamado a cada quadro, então tem de ser barato — ler de uma lista já pronta, não
	 * montá-la de novo a partir de pacotes.
	 */
	protected abstract List<T> rows();

	/** Desenha uma linha. {@code x, y} é o canto dela; a largura é a da área de conteúdo. */
	protected abstract void renderRow(DrawContext context, T row, int x, int y, int width, boolean hovered);

	/**
	 * Uma linha foi clicada.
	 *
	 * @param localX onde, dentro da linha — para linhas com mais de uma área clicável
	 */
	protected abstract void onRowClicked(T row, int index, double localX);

	/** O que dizer quando não há linha nenhuma. */
	protected Text emptyText() {
		return Text.translatable("screen.pokebook.tab.empty");
	}

	/** Altura reservada acima da lista, para o que a tela quiser pôr ali. */
	protected int headerHeight() {
		return 0;
	}

	/** Desenha o que vai acima da lista, se houver. */
	protected void renderHeader(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
	}

	/** Quantas fileiras de botões há embaixo. */
	protected int footerRows() {
		return 0;
	}

	// ------------------------------------------------------------------ medidas

	protected int headerTop() {
		return contentTop() + 2;
	}

	protected int listTop() {
		return headerTop() + headerHeight() + (headerHeight() > 0 ? 4 : 0);
	}

	protected int listBottom() {
		return contentY() + contentHeight() - footerRows() * (BUTTON_HEIGHT + BUTTON_GAP);
	}

	protected int listHeight() {
		return listBottom() - listTop();
	}

	/** O y de uma fileira de botões, contando de baixo: 0 é a última. */
	protected int footerY(int rowFromBottom) {
		return contentY() + contentHeight() - BUTTON_HEIGHT - rowFromBottom * (BUTTON_HEIGHT + BUTTON_GAP);
	}

	/**
	 * Põe botões lado a lado numa fileira de baixo, dividindo a largura igualmente.
	 *
	 * <p>O último fica com a sobra da divisão, para a fileira terminar exatamente na borda.
	 *
	 * @return os botões criados, na mesma ordem — para quem precisar mexer neles depois
	 */
	protected List<ButtonWidget> addFooterRow(int rowFromBottom, ButtonWidget.Builder... buttons) {
		int y = footerY(rowFromBottom);
		int each = (contentWidth() - BUTTON_GAP * (buttons.length - 1)) / buttons.length;
		int x = contentX();
		List<ButtonWidget> built = new ArrayList<>(buttons.length);
		for (int i = 0; i < buttons.length; i++) {
			int width = i == buttons.length - 1 ? contentX() + contentWidth() - x : each;
			built.add(addDrawableChild(buttons[i].dimensions(x, y, width, BUTTON_HEIGHT).build()));
			x += each + BUTTON_GAP;
		}
		return built;
	}

	private int maxScroll(int count) {
		return Math.max(0, count * ROW_HEIGHT - listHeight());
	}

	/** Volta ao topo — para quando o conteúdo muda por inteiro, como ao trocar de aba. */
	protected void resetScroll() {
		scroll = 0;
	}

	// ------------------------------------------------------------------ desenho

	/**
	 * Pede uma dica flutuante junto ao mouse neste quadro.
	 *
	 * <p>Desenhada só no fim, fora do recorte da lista: dentro dele a dica seria cortada na
	 * borda da área rolável, justamente onde ficam os botões pequenos que precisam dela.
	 */
	protected void tooltip(Text text) {
		pendingTooltip = text;
	}

	/** O mouse está dentro deste retângulo? Para as áreas clicáveis desenhadas à mão. */
	protected boolean isOver(int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		pendingTooltip = null;

		renderList(context, mouseX, mouseY);

		if (pendingTooltip != null) {
			context.drawTooltip(textRenderer, pendingTooltip, mouseX, mouseY);
		}
	}

	private void renderList(DrawContext context, int mouseX, int mouseY) {
		int x = contentX();
		int width = contentWidth();

		if (headerHeight() > 0) {
			renderHeader(context, x, headerTop(), width, mouseX, mouseY);
			outline(context, x, headerTop(), width, headerHeight(), DEBUG_AREA);
		}

		List<T> rows = rows();
		// A lista pode ter encolhido desde o último quadro (um pacote chegou, uma linha saiu):
		// sem reajustar, a rolagem apontaria para além do fim e a tela pareceria vazia.
		scroll = MathHelper.clamp(scroll, 0, maxScroll(rows.size()));

		if (rows.isEmpty()) {
			Text empty = emptyText();
			if (empty != null) {
				centered(context, empty, listTop() + 16, COLOR_MUTED);
			}
			return;
		}

		context.enableScissor(x, listTop(), x + width, listBottom());

		int rowY = listTop() - scroll;
		for (T row : rows) {
			// Linhas fora da área não são desenhadas: o recorte as esconderia de qualquer
			// jeito, e numa lista longa isso é trabalho à toa a cada quadro.
			if (rowY + ROW_HEIGHT > listTop() && rowY < listBottom()) {
				boolean hovered = mouseX >= x && mouseX < x + width
					&& mouseY >= Math.max(rowY, listTop())
					&& mouseY < Math.min(rowY + ROW_HEIGHT, listBottom());
				if (hovered) {
					context.fill(x, rowY, x + width, rowY + ROW_HEIGHT, HOVER);
				}
				renderRow(context, row, x, rowY, width, hovered);
				outline(context, x, rowY, width, ROW_HEIGHT, DEBUG_ROW);
			}
			rowY += ROW_HEIGHT;
		}

		context.disableScissor();
		outline(context, x, listTop(), width, listHeight(), DEBUG_AREA);
	}

	/** Texto centralizado na largura da área de conteúdo, sem sombra. */
	protected void centered(DrawContext context, Text text, int y, int color) {
		context.drawText(textRenderer, text,
			contentX() + (contentWidth() - textRenderer.getWidth(text)) / 2, y, color, false);
	}

	/** Texto alinhado à direita da linha. Devolve o x onde ele começa, para o que vier antes parar ali. */
	protected int rightAligned(DrawContext context, Text text, int rowX, int rowY, int width, int color) {
		int textX = rowX + width - textRenderer.getWidth(text);
		context.drawText(textRenderer, text, textX, rowY + (ROW_HEIGHT - textRenderer.fontHeight) / 2, color, false);
		return textX;
	}

	// ------------------------------------------------------------------ interação

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = contentX();
		if (button != 0 || mouseX < x || mouseX >= x + contentWidth()
			|| mouseY < listTop() || mouseY >= listBottom()) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		// O deslocamento da rolagem entra na conta: depois de rolar, a linha de cima não é a
		// primeira da lista.
		int index = (int) ((mouseY - listTop() + scroll) / ROW_HEIGHT);
		List<T> rows = rows();
		if (index < 0 || index >= rows.size()) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		onRowClicked(rows.get(index), index, mouseX - x);
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int max = maxScroll(rows().size());
		if (max > 0) {
			scroll = MathHelper.clamp(scroll - (int) (verticalAmount * ROW_HEIGHT / 2), 0, max);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
}
