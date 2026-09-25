package io.github.lxxz.pokebook.client.notify;

import io.github.lxxz.pokebook.client.screen.AppIcons;
import io.github.lxxz.pokebook.notify.NotificationKind;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

/**
 * O aviso que desliza no canto da tela, no lugar onde o jogo mostra conquistas.
 *
 * <p>É um {@link Toast} do vanilla, e não uma camada de HUD nossa: o jogo já empilha,
 * anima a entrada e a saída, toca o som de deslizar e respeita a opção de duração de
 * notificações do jogador. Refazer isso seria refazer o que o jogador já conhece.
 *
 * <p>As cores são as do chassi do aparelho — fundo escuro, texto claro —, e não as da tela
 * acesa. O aviso aparece sobre o mundo, que pode ser qualquer cor; um fundo claro sumiria
 * na neve.
 */
public class PhoneToast implements Toast {
	/** Quanto fica na tela, antes do multiplicador de acessibilidade do jogador. */
	private static final long DISPLAY_MS = 5000L;

	private static final int WIDTH = 160;
	private static final int HEIGHT = 32;
	private static final int ICON = 16;
	private static final int TEXT_X = 26;

	private static final int BACKGROUND = 0xF0262E30;
	private static final int BORDER = 0xFF5E696B;
	private static final int TITLE = 0xFFFFFFFF;
	private static final int BODY = 0xFFC3C6C6;

	private final NotificationKind kind;
	private final Text title;
	private final Text body;

	public PhoneToast(NotificationKind kind, Text title, Text body) {
		this.kind = kind;
		this.title = title;
		this.body = body;
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	/**
	 * Desenha na origem: quem posiciona o aviso na tela é o {@link ToastManager}, que já
	 * deslocou a matriz antes de chamar.
	 *
	 * @param startTime quanto tempo o aviso já está visível, em milissegundos — é assim que o
	 *                  vanilla chama, apesar do nome
	 */
	@Override
	public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
		TextRenderer textRenderer = manager.getClient().textRenderer;

		context.fill(0, 0, WIDTH, HEIGHT, BORDER);
		context.fill(1, 1, WIDTH - 1, HEIGHT - 1, BACKGROUND);

		// O ícone de app, reduzido à metade: os desenhos são de 32 px, e reduzir por fator
		// inteiro não borra.
		context.drawGuiTexture(AppIcons.forNotification(kind), 5, (HEIGHT - ICON) / 2, ICON, ICON);

		int maxWidth = WIDTH - TEXT_X - 5;
		context.drawText(textRenderer, textRenderer.trimToWidth(title.getString(), maxWidth),
			TEXT_X, 7, TITLE, false);
		context.drawText(textRenderer, textRenderer.trimToWidth(body.getString(), maxWidth),
			TEXT_X, 18, BODY, false);

		return startTime >= DISPLAY_MS * manager.getNotificationDisplayTimeMultiplier()
			? Visibility.HIDE
			: Visibility.SHOW;
	}
}
