package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.notify.ClientNotifications;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * O histórico da central de notificações: o que aconteceu enquanto o jogador não olhava.
 *
 * <p>É aqui que aparecem os avisos que o "não perturbe" guardou em silêncio — a ligação
 * perdida durante o modo avião é o caso que justifica a tela existir.
 *
 * <p>Cada linha mostra o ícone do app de origem, o título e quanto tempo faz. O corpo
 * aparece como dica ao passar o mouse, porque não cabe na linha junto com o resto.
 */
public class NotificationsScreen extends ScrollListScreen<ClientNotifications.Entry> {
	private static final int ICON = 16;

	public NotificationsScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.notifications"), session);
		// Abrir a lista é ler tudo: o número no ícone da tela inicial volta a zero.
		ClientNotifications.markRead();
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	@Override
	protected int footerRows() {
		return 1;
	}

	@Override
	protected void initPanel() {
		addFooterRow(0, ButtonWidget.builder(Text.translatable("screen.pokebook.notifications.clear"), button -> {
			ClientNotifications.clear();
			resetScroll();
		}));
	}

	@Override
	protected Text emptyText() {
		return Text.translatable("screen.pokebook.notifications.empty");
	}

	@Override
	protected List<ClientNotifications.Entry> rows() {
		return ClientNotifications.history();
	}

	@Override
	protected void renderRow(DrawContext context, ClientNotifications.Entry entry, int x, int y, int width, boolean hovered) {
		// O desenho do ícone é branco; tingido com a cor de destaque para aparecer no fundo claro.
		int color = COLOR_ACCENT;
		context.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);
		context.drawGuiTexture(AppIcons.forNotification(entry.kind()), x, y + (ROW_HEIGHT - ICON) / 2, ICON, ICON);
		// ⚠️ Estado global do quadro: sem voltar ao branco, o resto da tela sai tingido.
		context.setShaderColor(1f, 1f, 1f, 1f);

		int agoX = rightAligned(context, ago(entry.timeMillis()), x, y, width, COLOR_MUTED);
		int titleX = x + ICON + 4;
		context.drawText(textRenderer, textRenderer.trimToWidth(entry.title().getString(), agoX - 4 - titleX),
			titleX, y + (ROW_HEIGHT - textRenderer.fontHeight) / 2, COLOR_TEXT, false);

		if (hovered) {
			tooltip(entry.body());
		}
	}

	/** "agora", "5 min", "2 h" — o suficiente para saber se foi agora há pouco ou há muito. */
	private static Text ago(long timeMillis) {
		long minutes = (System.currentTimeMillis() - timeMillis) / 60_000;
		if (minutes < 1) {
			return Text.translatable("screen.pokebook.notifications.now");
		}
		if (minutes < 60) {
			return Text.translatable("screen.pokebook.notifications.minutes", minutes);
		}
		return Text.translatable("screen.pokebook.notifications.hours", minutes / 60);
	}

	@Override
	protected void onRowClicked(ClientNotifications.Entry entry, int index, double localX) {
		// Só leitura; o corpo já aparece ao passar o mouse.
	}
}
