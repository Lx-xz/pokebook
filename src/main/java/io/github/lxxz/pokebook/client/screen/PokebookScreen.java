package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.ClosePokebookPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * A tela do pokébook.
 *
 * <p>Por enquanto mostra só o apelido do jogador — que veio do servidor, não do cliente.
 * O objetivo deste marco não é a tela em si: é provar o caminho completo de ida e volta
 * antes de colocar dados de verdade nele.
 *
 * <p>O visual é desenhado por código, sem textura. Assim dá para ajustar proporção e
 * cores sem depender de pixel art, e trocar por uma textura depois é mudança localizada.
 */
public class PokebookScreen extends Screen {
	/** Mesma folga que baú e fornalha usam (8 blocos). */
	private static final double MAX_DISTANCE_SQUARED = 64.0;

	private static final int PANEL_WIDTH = 220;
	private static final int PANEL_HEIGHT = 140;

	private static final int COLOR_BACKGROUND = 0xF00E1622;
	private static final int COLOR_BORDER = 0xFF29B6D8;
	private static final int COLOR_TEXT = 0xFFE6F6FB;
	private static final int COLOR_ACCENT = 0xFF7FE3F5;

	private final BlockPos pos;
	private final String nick;

	/** {@link #removed()} pode ser chamado mais de uma vez; o aviso ao servidor não. */
	private boolean notifiedServer;

	public PokebookScreen(BlockPos pos, String nick) {
		super(Text.translatable("screen.pokebook.title"));
		this.pos = pos;
		this.nick = nick;
	}

	@Override
	public boolean shouldPause() {
		// Um notebook é usado dentro do mundo, não fora dele. Pausar também atrapalharia
		// a atualização ao vivo das missões, quando ela existir.
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

		int x = (width - PANEL_WIDTH) / 2;
		int y = (height - PANEL_HEIGHT) / 2;

		context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, COLOR_BACKGROUND);
		context.drawBorder(x, y, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);

		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, y + 12, COLOR_TEXT);
		context.drawTextWithShadow(textRenderer,
			Text.translatable("screen.pokebook.logged_as", nick), x + 14, y + 40, COLOR_ACCENT);
		context.drawTextWithShadow(textRenderer,
			Text.translatable("screen.pokebook.no_missions"), x + 14, y + 58, COLOR_TEXT);
	}

	@Override
	public void removed() {
		// Fechar a tela é o que apaga a luz do bloco, e só o servidor pode mexer nisso.
		if (!notifiedServer && client != null && client.getNetworkHandler() != null) {
			notifiedServer = true;
			ClientPlayNetworking.send(new ClosePokebookPayload(pos));
		}
		super.removed();
	}
}
