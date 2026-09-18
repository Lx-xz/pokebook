package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.MissionEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Tela inicial do pokébook.
 *
 * <p>Existe com um botão só de propósito. A navegação é o que define a forma dos pacotes,
 * e encaixar um menu depois significaria mexer num fluxo de dados já funcionando. Com a
 * moldura pronta, a aba social entra sem tocar em nada.
 */
public class PokebookMenuScreen extends PokebookScreenBase {
	private final List<MissionEntry> missions;

	public PokebookMenuScreen(BlockPos pos, String nick, List<MissionEntry> missions) {
		super(Text.translatable("screen.pokebook.title"), pos, nick);
		this.missions = missions;
	}

	@Override
	protected void initPanel() {
		int x = panelX() + 20;
		int y = panelY() + 56;
		int buttonWidth = PANEL_WIDTH - 40;

		addDrawableChild(ButtonWidget.builder(
			Text.translatable("screen.pokebook.missions"),
			button -> navigateTo(new MissionsScreen(pos, nick, missions))
		).dimensions(x, y, buttonWidth, 20).build());

		ButtonWidget social = ButtonWidget.builder(
			Text.translatable("screen.pokebook.social"), button -> {}
		).dimensions(x, y + 26, buttonWidth, 20).build();
		social.active = false;
		social.setTooltip(Tooltip.of(Text.translatable("screen.pokebook.soon")));
		addDrawableChild(social);
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		context.drawText(textRenderer,
			Text.translatable("screen.pokebook.logged_as", nick),
			panelX() + 14, panelY() + 32, COLOR_ACCENT, false);
	}
}
