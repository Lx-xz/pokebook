package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.ClosePokebookPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * O que toda tela do pokébook compartilha: a moldura, o fechamento por distância e o
 * aviso ao servidor.
 *
 * <p><b>A armadilha que isto resolve:</b> {@code removed()} dispara em qualquer troca de
 * tela, inclusive ao navegar do menu para as missões. Sem cuidado, sair do menu mandaria
 * o pacote de fechamento e apagaria a luz do bloco no meio da navegação. Por isso a
 * troca entre telas do pokébook passa por {@link #navigateTo}, que marca a sessão como
 * contínua. Só a saída de verdade avisa o servidor.
 */
public abstract class PokebookScreenBase extends Screen {
	/** Mesma folga que baú e fornalha usam (8 blocos). */
	private static final double MAX_DISTANCE_SQUARED = 64.0;

	protected static final int PANEL_WIDTH = 240;
	protected static final int PANEL_HEIGHT = 160;

	protected static final int COLOR_BACKGROUND = 0xF00E1622;
	protected static final int COLOR_BORDER = 0xFF29B6D8;
	protected static final int COLOR_TEXT = 0xFFE6F6FB;
	protected static final int COLOR_ACCENT = 0xFF7FE3F5;
	protected static final int COLOR_DONE = 0xFF7BD88F;
	protected static final int COLOR_MUTED = 0xFF6E8391;

	protected final BlockPos pos;
	protected final String nick;

	/** Verdadeiro enquanto trocamos para outra tela do próprio pokébook. */
	private boolean keepingSession;

	protected PokebookScreenBase(Text title, BlockPos pos, String nick) {
		super(title);
		this.pos = pos;
		this.nick = nick;
	}

	protected int panelX() {
		return (width - PANEL_WIDTH) / 2;
	}

	protected int panelY() {
		return (height - PANEL_HEIGHT) / 2;
	}

	/** Troca para outra tela do pokébook sem encerrar a sessão com o bloco. */
	protected void navigateTo(PokebookScreenBase next) {
		keepingSession = true;
		if (client != null) {
			client.setScreen(next);
		}
	}

	@Override
	public boolean shouldPause() {
		// Um notebook é usado dentro do mundo, não fora dele.
		return false;
	}

	@Override
	public void tick() {
		// Sem ScreenHandler, ninguém fecha a tela por nós quando o jogador se afasta.
		if (client == null || client.player == null) {
			return;
		}
		if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > MAX_DISTANCE_SQUARED) {
			close();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x = panelX();
		int y = panelY();
		context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, COLOR_BACKGROUND);
		context.drawBorder(x, y, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, y + 10, COLOR_TEXT);
		renderPanel(context, mouseX, mouseY, delta);
	}

	/** Conteúdo próprio de cada tela, desenhado dentro da moldura. */
	protected abstract void renderPanel(DrawContext context, int mouseX, int mouseY, float delta);

	@Override
	public void removed() {
		if (!keepingSession && client != null && client.getNetworkHandler() != null) {
			ClientPlayNetworking.send(new ClosePokebookPayload(pos));
		}
		super.removed();
	}
}
