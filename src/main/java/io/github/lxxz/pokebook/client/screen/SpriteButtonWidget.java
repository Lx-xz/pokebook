package io.github.lxxz.pokebook.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Um botão que é só o desenho: sem moldura, sem fundo, sem texto.
 *
 * <p>É o que os botões de canto precisam. O {@link ButtonWidget} do vanilla desenha a
 * moldura cinza de sempre, que destoa de uma interface com arte própria — aqui o widget
 * existe pelo clique e pelo foco de teclado, e o desenho é a textura.
 *
 * <p>O rótulo continua sendo passado, e não é enfeite: é o que o leitor de tela anuncia e
 * o que a dica de tecla mostra. Ele só não é <em>desenhado</em>.
 *
 * <p><b>A textura deve ser branca</b> — ver {@code CLAUDE.md}. A cor sai daqui, e é o que
 * permite o mesmo arquivo servir ao estado normal e ao estado sob o mouse.
 */
public class SpriteButtonWidget extends ButtonWidget {
	private final Identifier sprite;

	public SpriteButtonWidget(int x, int y, int size, Identifier sprite, Text label, PressAction onPress) {
		super(x, y, size, size, label, onPress, DEFAULT_NARRATION_SUPPLIER);
		this.sprite = sprite;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		int tint = !active
			? PokebookScreenBase.COLOR_MUTED
			: (isHovered() ? PokebookScreenBase.COLOR_ACCENT : PokebookScreenBase.COLOR_TEXT);

		context.setShaderColor(
			((tint >> 16) & 0xFF) / 255f,
			((tint >> 8) & 0xFF) / 255f,
			(tint & 0xFF) / 255f,
			1f);

		context.drawGuiTexture(sprite, getX(), getY(), getWidth(), getHeight());

		// ⚠️ Estado global do quadro: sem voltar ao branco, tudo o que for desenhado depois
		// sai tingido.
		context.setShaderColor(1f, 1f, 1f, 1f);
	}
}
