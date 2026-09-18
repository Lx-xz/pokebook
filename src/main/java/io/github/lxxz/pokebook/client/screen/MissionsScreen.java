package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.ClaimRewardPayload;
import io.github.lxxz.pokebook.network.MissionEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Lista de missões, em abas e com rolagem.
 *
 * <p><b>As abas são por estado, não por assunto.</b> Toda missão está num de três
 * lugares: em andamento, pronta para resgate, ou concluída. Separar assim põe a única
 * coisa acionável — resgatar — numa aba própria, em vez de enterrá-la no meio de uma
 * lista onde a maioria das linhas não pede nada. Abas por assunto (matar, capturar)
 * seriam taxonomia: organizam, mas não dizem o que fazer a seguir.
 *
 * <p>O filtro é do <b>cliente</b>. O servidor já manda a lista inteira e cada linha já
 * carrega o que é preciso para classificá-la, então trocar de aba não toca a rede. Passar
 * a mandar duas listas só compensaria se a lista crescesse a ponto de não valer a pena
 * enviá-la inteira, ou se "concluídas" ganhasse dado que "em andamento" não tem.
 *
 * <p>As linhas <b>não são widgets</b>. Widget tem posição fixa, e numa lista que rola a
 * posição muda a cada quadro — manter um widget por linha significaria reposicionar todos
 * a cada rolagem. Desenhar as linhas à mão e tratar o clique com o deslocamento aplicado
 * é menos código e não pode dessincronizar. As abas, que não rolam, continuam widgets.
 */
public class MissionsScreen extends PokebookScreenBase {
	private static final int ROW_HEIGHT = 26;
	private static final int TAB_HEIGHT = 14;
	private static final int LIST_TOP_INSET = 42;
	private static final int LIST_BOTTOM_INSET = 10;

	private static final int CLAIM_WIDTH = 52;
	private static final int CLAIM_HEIGHT = 18;

	/** Os três estados possíveis de uma missão, que são exatamente as três abas. */
	private enum Tab {
		IN_PROGRESS("screen.pokebook.tab.in_progress", entry -> !entry.complete()),
		CLAIMABLE("screen.pokebook.tab.claimable", MissionEntry::claimable),
		DONE("screen.pokebook.tab.done", MissionEntry::claimed);

		final String key;
		final Predicate<MissionEntry> filter;

		Tab(String key, Predicate<MissionEntry> filter) {
			this.key = key;
			this.filter = filter;
		}
	}

	private List<MissionEntry> missions;
	private Tab tab = Tab.IN_PROGRESS;
	private int scroll;

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
		Tab[] tabs = Tab.values();
		int tabWidth = (PANEL_WIDTH - 40) / tabs.length;
		int tabX = panelX() + 20;

		for (Tab candidate : tabs) {
			ButtonWidget button = ButtonWidget.builder(Text.translatable(candidate.key), b -> selectTab(candidate))
				.dimensions(tabX, panelY() + 24, tabWidth, TAB_HEIGHT)
				.build();
			// A aba atual não é clicável: já estamos nela, e desabilitada ela também fica
			// visualmente distinta das outras sem precisar de sprite próprio.
			button.active = candidate != tab;
			addDrawableChild(button);
			tabX += tabWidth;
		}
	}

	private void selectTab(Tab next) {
		tab = next;
		scroll = 0;
		clearAndInit();
	}

	private List<MissionEntry> visible() {
		return missions.stream().filter(tab.filter).toList();
	}

	private int listTop() {
		return panelY() + LIST_TOP_INSET;
	}

	private int listHeight() {
		return PANEL_HEIGHT - LIST_TOP_INSET - LIST_BOTTOM_INSET;
	}

	private int maxScroll() {
		return Math.max(0, visible().size() * ROW_HEIGHT - listHeight());
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		List<MissionEntry> entries = visible();
		int x = panelX();

		if (entries.isEmpty()) {
			Text empty = Text.translatable("screen.pokebook.tab.empty");
			context.drawText(textRenderer, empty,
				x + (PANEL_WIDTH - textRenderer.getWidth(empty)) / 2, listTop() + 16, COLOR_MUTED, false);
			return;
		}

		// Recorta ao retângulo da lista: sem isto, a linha que está saindo por cima
		// continuaria desenhada sobre as abas e sobre o título.
		context.enableScissor(x, listTop(), x + PANEL_WIDTH, listTop() + listHeight());

		int rowY = listTop() - scroll;
		for (MissionEntry entry : entries) {
			renderRow(context, entry, x, rowY, mouseX, mouseY);
			rowY += ROW_HEIGHT;
		}

		context.disableScissor();

		if (maxScroll() > 0) {
			renderScrollbar(context, x, entries.size());
		}
	}

	private void renderRow(DrawContext context, MissionEntry entry, int x, int rowY, int mouseX, int mouseY) {
		context.drawItem(entry.reward(), x + 10, rowY + 4);
		context.drawItemInSlot(textRenderer, entry.reward(), x + 10, rowY + 4);

		int titleColor = entry.claimed() ? COLOR_MUTED : (entry.complete() ? COLOR_DONE : COLOR_TEXT);
		context.drawText(textRenderer, entry.title(), x + 32, rowY + 2, titleColor, false);

		Text status = entry.claimed()
			? Text.translatable("screen.pokebook.claimed")
			: Text.literal(entry.count() + "/" + entry.required());
		context.drawText(textRenderer, status, x + 32, rowY + 13,
			entry.claimed() ? COLOR_MUTED : COLOR_ACCENT, false);

		if (entry.claimable()) {
			int buttonX = x + PANEL_WIDTH - CLAIM_WIDTH - 10;
			int buttonY = rowY + 3;
			boolean hovered = mouseX >= buttonX && mouseX < buttonX + CLAIM_WIDTH
				&& mouseY >= buttonY && mouseY < buttonY + CLAIM_HEIGHT
				&& mouseY >= listTop() && mouseY < listTop() + listHeight();

			context.fill(buttonX, buttonY, buttonX + CLAIM_WIDTH, buttonY + CLAIM_HEIGHT,
				hovered ? 0xFF1B6B2A : 0xFF0D5A70);
			Text label = Text.translatable("screen.pokebook.claim");
			context.drawText(textRenderer, label,
				buttonX + (CLAIM_WIDTH - textRenderer.getWidth(label)) / 2, buttonY + 5, 0xFFFFFFFF, false);
		}
	}

	/** Barra fina à direita, só para dizer que há mais coisa e onde estamos. */
	private void renderScrollbar(DrawContext context, int x, int rowCount) {
		int trackX = x + PANEL_WIDTH - 6;
		int trackTop = listTop();
		int trackHeight = listHeight();

		int thumbHeight = Math.max(12, trackHeight * trackHeight / (rowCount * ROW_HEIGHT));
		int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll();

		context.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0x30000000);
		context.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFF0D5A70);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (maxScroll() > 0) {
			scroll = MathHelper.clamp(scroll - (int) (verticalAmount * ROW_HEIGHT / 2), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && mouseY >= listTop() && mouseY < listTop() + listHeight()) {
			int x = panelX();
			int buttonX = x + PANEL_WIDTH - CLAIM_WIDTH - 10;
			int rowY = listTop() - scroll;

			for (MissionEntry entry : visible()) {
				if (entry.claimable()
					&& mouseX >= buttonX && mouseX < buttonX + CLAIM_WIDTH
					&& mouseY >= rowY + 3 && mouseY < rowY + 3 + CLAIM_HEIGHT) {
					ClientPlayNetworking.send(new ClaimRewardPayload(entry.id()));
					return true;
				}
				rowY += ROW_HEIGHT;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
