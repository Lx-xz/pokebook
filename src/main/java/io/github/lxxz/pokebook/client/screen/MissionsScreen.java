package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.ClaimRewardPayload;
import io.github.lxxz.pokebook.network.MissionEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de missões, com resgate.
 *
 * <p>Sem rolagem: são poucas missões e um widget de rolagem traz complexidade própria.
 * Quando passarem de caber na moldura, aí sim — é mudança contida.
 *
 * <p>O botão só pede; quem decide se o resgate vale é o servidor.
 */
public class MissionsScreen extends PokebookScreenBase {
	private static final int ROW_HEIGHT = 26;

	private List<MissionEntry> missions;

	public MissionsScreen(BlockPos pos, String nick, List<MissionEntry> missions) {
		super(Text.translatable("screen.pokebook.missions"), pos, nick);
		this.missions = new ArrayList<>(missions);
	}

	/** Chamado quando o servidor avisa que o progresso mudou, sem reabrir a tela. */
	public void update(List<MissionEntry> updated) {
		this.missions = new ArrayList<>(updated);
		clearAndInit();
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(pos, nick, missions);
	}

	@Override
	protected void initPanel() {
		int rowY = panelY() + 28;
		for (MissionEntry entry : missions) {
			if (entry.claimable()) {
				addDrawableChild(ButtonWidget.builder(
					Text.translatable("screen.pokebook.claim"),
					button -> ClientPlayNetworking.send(new ClaimRewardPayload(entry.id()))
				).dimensions(panelX() + PANEL_WIDTH - 62, rowY + 2, 52, 18).build());
			}
			rowY += ROW_HEIGHT;
		}
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		int x = panelX();
		int rowY = panelY() + 28;

		for (MissionEntry entry : missions) {
			context.drawItem(entry.reward(), x + 10, rowY + 4);
			context.drawItemInSlot(textRenderer, entry.reward(), x + 10, rowY + 4);

			int titleColor = entry.claimed() ? COLOR_MUTED : (entry.complete() ? COLOR_DONE : COLOR_TEXT);
			context.drawText(textRenderer, entry.title(), x + 32, rowY + 2, titleColor, false);

			Text status = entry.claimed()
				? Text.translatable("screen.pokebook.claimed")
				: Text.literal(entry.count() + "/" + entry.required());
			context.drawText(textRenderer, status, x + 32, rowY + 13,
				entry.claimed() ? COLOR_MUTED : COLOR_ACCENT, false);

			rowY += ROW_HEIGHT;
		}
	}
}
