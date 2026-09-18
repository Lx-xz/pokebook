package io.github.lxxz.pokebook.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Um ícone de tela inicial: quadrado com o desenho, rótulo embaixo.
 *
 * <p>É um {@link ButtonWidget} e não um retângulo desenhado à mão porque o menu
 * <b>não rola</b> — a armadilha registrada no {@code CLAUDE.md} vale para listas que
 * rolam, e aqui não há nenhuma. Sendo widget, ganha foco de teclado, narração e
 * tratamento de clique de graça.
 *
 * <p><b>O desenho de hoje é provisório e de propósito.</b> O quadrado é um preenchimento
 * de cor sólida e o ícone é um caractere da fonte do jogo. Quando as texturas existirem,
 * só {@link #renderWidget} muda: o {@code fill} vira {@code drawGuiTexture} do fundo e o
 * {@code drawText} do glifo vira {@code drawGuiTexture} do ícone. O resto da tela — a
 * grade, as posições, os cliques — não precisa saber que mudou.
 *
 * <p>O glifo é um caractere BMP resolvido pela fonte de reserva do Minecraft (GNU
 * Unifont). Se algum aparecer como quadradinho vazio, é porque a fonte não o cobre — e a
 * resposta é a textura, não outro caractere.
 */
public class IconTileWidget extends ButtonWidget {
	/** Folga entre o quadrado do ícone e o rótulo. */
	private static final int LABEL_GAP = 2;

	private final String glyph;
	private final int tileSize;

	public IconTileWidget(int x, int y, int tileSize, int height, String glyph, Text label, PressAction onPress) {
		super(x, y, tileSize, height, label, onPress, DEFAULT_NARRATION_SUPPLIER);
		this.glyph = glyph;
		this.tileSize = tileSize;
	}

	/** A altura total que um ícone ocupa: o quadrado, a folga e a linha do rótulo. */
	public static int heightFor(int tileSize, int fontHeight) {
		return tileSize + LABEL_GAP + fontHeight;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;

		int x = getX();
		int y = getY();

		// Fundo do ícone. Vira drawGuiTexture quando houver sprite.
		int background = !active
			? 0x40FFFFFF
			: (isHovered() ? 0xFFFFFFFF : 0xE6FFFFFF);
		context.fill(x, y, x + tileSize, y + tileSize, background);

		int tint = active ? PokebookScreenBase.COLOR_ACCENT : PokebookScreenBase.COLOR_MUTED;

		// O glifo centrado no quadrado. Vira o sprite do ícone quando houver.
		int glyphX = x + (tileSize - textRenderer.getWidth(glyph)) / 2;
		int glyphY = y + (tileSize - textRenderer.fontHeight) / 2;
		context.drawText(textRenderer, glyph, glyphX, glyphY, tint, false);

		// O rótulo embaixo, cortado se não couber: é melhor "Missõ..." do que texto
		// invadindo o ícone vizinho.
		Text label = getMessage();
		String trimmed = textRenderer.trimToWidth(label.getString(), tileSize);
		int labelX = x + (tileSize - textRenderer.getWidth(trimmed)) / 2;
		context.drawText(textRenderer, trimmed, labelX, y + tileSize + LABEL_GAP,
			active ? PokebookScreenBase.COLOR_TEXT : PokebookScreenBase.COLOR_MUTED, false);
	}

}
