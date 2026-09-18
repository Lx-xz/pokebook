package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.SocialEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Quem está na frente.
 *
 * <p>É o primeiro degrau da aba social: <b>ler</b> o progresso dos outros. Nenhum sistema
 * novo — o dado já é persistido por jogador, e isto é uma consulta e uma tela. O segundo
 * degrau, mensagens entre jogadores, é um mod inteiro por si só e não está aqui.
 *
 * <p>A tela <b>nasce vazia</b> e se preenche quando a resposta chega. O pedido só sai ao
 * abrir esta aba, não ao abrir o pokébook: a maioria das aberturas nunca chega aqui, e
 * mandar a lista de todo mundo sempre seria pagar por algo usado às vezes. O custo é este
 * quadro de espera, que num servidor local ninguém vê.
 *
 * <p>Só jogadores conectados aparecem — ver {@code MissionService.socialSnapshot}.
 */
public class SocialScreen extends PokebookScreenBase {
	private static final int ROW_HEIGHT = 22;
	private static final int LIST_TOP_INSET = 30;
	private static final int LIST_BOTTOM_INSET = 10;

	/** Guardadas só para reconstruir o menu ao voltar. */
	private final List<MissionEntry> missions;

	private List<SocialEntry> players = List.of();
	private boolean loaded;
	private int scroll;

	public SocialScreen(BlockPos pos, String nick, List<MissionEntry> missions) {
		super(Text.translatable("screen.pokebook.social"), pos, nick);
		this.missions = missions;
	}

	/** Chamado quando a resposta do servidor chega. */
	public void update(List<SocialEntry> updated) {
		this.players = new ArrayList<>(updated);
		this.loaded = true;
		this.scroll = 0;
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(pos, nick, missions);
	}

	private int listTop() {
		return panelY() + LIST_TOP_INSET;
	}

	private int listHeight() {
		return PANEL_HEIGHT - LIST_TOP_INSET - LIST_BOTTOM_INSET;
	}

	private int maxScroll() {
		return Math.max(0, players.size() * ROW_HEIGHT - listHeight());
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		int x = panelX();

		if (!loaded) {
			centered(context, Text.translatable("screen.pokebook.loading"), x, listTop() + 16, COLOR_MUTED);
			return;
		}
		if (players.isEmpty()) {
			centered(context, Text.translatable("screen.pokebook.social.empty"), x, listTop() + 16, COLOR_MUTED);
			return;
		}

		context.enableScissor(x, listTop(), x + PANEL_WIDTH, listTop() + listHeight());

		int rowY = listTop() - scroll;
		for (SocialEntry entry : players) {
			boolean self = entry.name().equals(nick);
			int nameColor = self ? COLOR_ACCENT : COLOR_TEXT;

			context.drawText(textRenderer, Text.literal(entry.name()), x + 12, rowY + 2, nameColor, false);

			Text summary = Text.translatable("screen.pokebook.social.summary",
				entry.completed(), entry.total(), entry.claimed());
			context.drawText(textRenderer, summary, x + 12, rowY + 12, COLOR_MUTED, false);

			// Barrinha de progresso: a comparação entre jogadores se lê de relance, sem
			// precisar ler os números de cada linha.
			int barWidth = PANEL_WIDTH - 24;
			int filled = entry.total() == 0 ? 0 : barWidth * entry.completed() / entry.total();
			context.fill(x + 12, rowY + 21, x + 12 + barWidth, rowY + 23, 0x30000000);
			if (filled > 0) {
				context.fill(x + 12, rowY + 21, x + 12 + filled, rowY + 23, COLOR_DONE);
			}

			rowY += ROW_HEIGHT;
		}

		context.disableScissor();
	}

	private void centered(DrawContext context, Text text, int x, int y, int color) {
		context.drawText(textRenderer, text, x + (PANEL_WIDTH - textRenderer.getWidth(text)) / 2, y, color, false);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (maxScroll() > 0) {
			scroll = MathHelper.clamp(scroll - (int) (verticalAmount * ROW_HEIGHT / 2), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
}
