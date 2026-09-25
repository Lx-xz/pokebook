package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.hud.Navigation;
import io.github.lxxz.pokebook.client.map.AreaMap;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.phone.Waypoint;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * O mapa da área em volta do pokébook, com os pontos de interesse por cima.
 *
 * <p><b>Só no pokébook.</b> É a "tela grande" da divisão entre os aparelhos: consultar o
 * entorno é coisa de estação, e a tela do poképhone é pequena demais para um mapa legível.
 *
 * <p>A imagem vem de {@link AreaMap}, que a desenha aos poucos; esta tela só anda a geração a
 * cada quadro e põe os marcadores. Passar o mouse mostra a coordenada; clicar num ponto de
 * interesse liga a seta do HUD para ele.
 */
public class MapScreen extends PokebookScreenBase {
	private static final int FOOTER = 12;
	/** Os ícones dos pontos saem em metade do tamanho de item: 8 px. */
	private static final float ICON_SCALE = 0.5f;
	private static final int ICON_HALF = 4;

	private static final int PLAYER = 0xFFFFFFFF;
	private static final int PLAYER_OUTLINE = 0xFF000000;
	private static final int FRAME = 0xFF102028;

	public MapScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.map"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	@Override
	protected void initPanel() {
		if (client == null || client.player == null) {
			return;
		}
		// Centrado no pokébook, que é onde a estação está; o jogador está a poucos passos.
		BlockPos center = session.pos().orElse(client.player.getBlockPos());
		AreaMap.begin(client, center);
	}

	// ------------------------------------------------------------------ geometria

	private int side() {
		return Math.max(16, Math.min(contentWidth(), contentY() + contentHeight() - FOOTER - (contentTop() + 2)));
	}

	private int mapX() {
		return contentX() + (contentWidth() - side()) / 2;
	}

	private int mapY() {
		return contentTop() + 2;
	}

	/** Um ponto do mundo na tela; {@code null} se estiver fora do mapa. */
	private int[] toScreen(double worldX, double worldZ) {
		double u = (worldX - AreaMap.originX()) / AreaMap.SIZE;
		double v = (worldZ - AreaMap.originZ()) / AreaMap.SIZE;
		if (u < 0 || u >= 1 || v < 0 || v >= 1) {
			return null;
		}
		return new int[] {mapX() + (int) (u * side()), mapY() + (int) (v * side())};
	}

	private boolean overMap(double mouseX, double mouseY) {
		return mouseX >= mapX() && mouseX < mapX() + side() && mouseY >= mapY() && mouseY < mapY() + side();
	}

	// ------------------------------------------------------------------ desenho

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		if (client == null || client.player == null || client.world == null) {
			return;
		}
		AreaMap.step(client);

		int side = side();
		int x = mapX();
		int y = mapY();
		outline(context, x - 1, y - 1, side + 2, side + 2, FRAME);
		// A sobrecarga longa: tamanho na tela ANTES de u,v. Ver o CLAUDE.md.
		context.drawTexture(AreaMap.TEXTURE, x, y, side, side, 0f, 0f,
			AreaMap.SIZE, AreaMap.SIZE, AreaMap.SIZE, AreaMap.SIZE);

		Waypoint hovered = drawWaypoints(context, mouseX, mouseY);
		drawPlayer(context);

		// Rodapé: a coordenada sob o mouse, ou o aviso de que ainda está desenhando.
		Text footer;
		if (!AreaMap.ready()) {
			footer = Text.translatable("screen.pokebook.map.drawing");
		} else if (overMap(mouseX, mouseY)) {
			int blockX = AreaMap.originX() + (mouseX - x) * AreaMap.SIZE / side;
			int blockZ = AreaMap.originZ() + (mouseY - y) * AreaMap.SIZE / side;
			footer = Text.translatable("screen.pokebook.map.coords", blockX, blockZ);
		} else {
			footer = Text.translatable("screen.pokebook.map.hint");
		}
		context.drawText(textRenderer, footer,
			contentX() + (contentWidth() - textRenderer.getWidth(footer)) / 2, y + side + 3, COLOR_MUTED, false);

		if (hovered != null) {
			context.drawTooltip(textRenderer, Text.literal(hovered.name()), mouseX, mouseY);
		}
	}

	/** Desenha os pontos desta dimensão e devolve o que está sob o mouse. */
	private Waypoint drawWaypoints(DrawContext context, int mouseX, int mouseY) {
		Waypoint hovered = null;
		for (Waypoint waypoint : ClientPhone.data().waypoints()) {
			int[] at = at(waypoint);
			if (at == null) {
				continue;
			}
			context.getMatrices().push();
			context.getMatrices().translate(at[0] - ICON_HALF, at[1] - ICON_HALF, 0);
			context.getMatrices().scale(ICON_SCALE, ICON_SCALE, 1f);
			context.drawItem(Registries.ITEM.get(waypoint.icon()).getDefaultStack(), 0, 0);
			context.getMatrices().pop();
			if (Math.abs(mouseX - at[0]) <= ICON_HALF && Math.abs(mouseY - at[1]) <= ICON_HALF) {
				hovered = waypoint;
			}
		}
		return hovered;
	}

	private int[] at(Waypoint waypoint) {
		if (client == null || client.world == null || !waypoint.pos().dimension().equals(client.world.getRegistryKey())) {
			return null;
		}
		BlockPos pos = waypoint.pos().pos();
		return toScreen(pos.getX() + 0.5, pos.getZ() + 0.5);
	}

	/** O jogador: um ponto com um traço para onde está olhando. */
	private void drawPlayer(DrawContext context) {
		int[] at = toScreen(client.player.getX(), client.player.getZ());
		if (at == null) {
			return;
		}
		// Yaw 0 é sul (+Z), que no mapa é para baixo; cresce no sentido horário visto de cima.
		float yaw = client.player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		int tipX = at[0] + Math.round(-MathHelper.sin(yaw) * 5);
		int tipY = at[1] + Math.round(MathHelper.cos(yaw) * 5);
		line(context, at[0], at[1], tipX, tipY, PLAYER_OUTLINE);
		context.fill(at[0] - 2, at[1] - 2, at[0] + 2, at[1] + 2, PLAYER_OUTLINE);
		context.fill(at[0] - 1, at[1] - 1, at[0] + 1, at[1] + 1, PLAYER);
	}

	/** Uma reta de pixels. Só para o traço de direção: curto, então o custo não importa. */
	private static void line(DrawContext context, int x0, int y0, int x1, int y1, int color) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		for (int i = 0; i <= steps; i++) {
			int px = x0 + (steps == 0 ? 0 : (x1 - x0) * i / steps);
			int py = y0 + (steps == 0 ? 0 : (y1 - y0) * i / steps);
			context.fill(px, py, px + 1, py + 1, color);
		}
	}

	// ------------------------------------------------------------------ interação

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && overMap(mouseX, mouseY)) {
			for (Waypoint waypoint : ClientPhone.data().waypoints()) {
				int[] at = at(waypoint);
				if (at != null && Math.abs(mouseX - at[0]) <= ICON_HALF && Math.abs(mouseY - at[1]) <= ICON_HALF) {
					Navigation.navigateTo(new Navigation.Fixed(waypoint.pos(), waypoint.name()));
					close();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
