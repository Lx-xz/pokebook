package io.github.lxxz.pokebook.client.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Um botão do vanilla que <b>encolhe o rótulo em vez de cortá-lo</b>.
 *
 * <p>Existe por causa das abas das missões. Três abas dividem a largura da tela, então a
 * largura de cada uma não é escolha — é a conta. E o {@link ButtonWidget} do vanilla,
 * quando o texto não cabe, corta pelas duas pontas: "Resgatar" apareceu como "esgata",
 * que não quer dizer nada.
 *
 * <p>A moldura continua sendo a do vanilla, de propósito. Só o desenho do texto muda, e é
 * o mínimo que resolve o defeito — quando as abas ganharem textura própria, é esta classe
 * que passa a desenhá-la, e as telas que a usam não precisam saber.
 *
 * <p><b>Por que sobrescrever {@code drawMessage} e não {@code renderWidget}:</b> o fundo,
 * o estado sob o mouse e o foco de teclado são desenhados pelo {@code renderWidget} do
 * vanilla, e recriá-los seria copiar código do jogo para mudar uma linha. O
 * {@code drawMessage} é o gancho que o próprio jogo oferece para exatamente isto.
 */
public class TabButtonWidget extends ButtonWidget {
	/** Folga de cada lado, para o texto não encostar na moldura do botão. */
	private static final int PADDING = 2;

	public TabButtonWidget(int x, int y, int width, int height, Text label, PressAction onPress) {
		super(x, y, width, height, label, onPress, DEFAULT_NARRATION_SUPPLIER);
	}

	@Override
	public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
		PokebookScreenBase.drawFittedLabel(context, textRenderer, getMessage().getString(),
			getX() + getWidth() / 2,
			getY() + (getHeight() - textRenderer.fontHeight) / 2,
			getWidth() - PADDING * 2,
			color);
	}
}
