package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.hud.Navigation;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.client.radar.Radar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/**
 * A tela do radar: os Pokémon por perto, com distância e direção.
 *
 * <p>A lista é refeita a cada tick, e não a cada quadro: percorrer as entidades do mundo é
 * barato, mas não precisa ser feito sessenta vezes por segundo para uma lista que se lê.
 *
 * <p>Tocar num Pokémon liga a seta do HUD para segui-lo enquanto ele estiver ao alcance.
 */
public class RadarScreen extends ScrollListScreen<Radar.Blip> {
	private static final int ARROW_WIDTH = 12;

	private List<Radar.Blip> blips = List.of();

	public RadarScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.radar"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	@Override
	protected void initPanel() {
		refresh();
	}

	@Override
	public void tick() {
		super.tick();
		refresh();
	}

	private void refresh() {
		if (client != null) {
			blips = Radar.scan(client);
		}
	}

	@Override
	protected Text emptyText() {
		return Text.translatable("screen.pokebook.radar.empty");
	}

	@Override
	protected List<Radar.Blip> rows() {
		return blips;
	}

	@Override
	protected void renderRow(DrawContext context, Radar.Blip blip, int x, int y, int width, boolean hovered) {
		int textY = y + (ROW_HEIGHT - textRenderer.fontHeight) / 2;

		String arrow = blip.arrow();
		context.drawText(textRenderer, arrow, x + (ARROW_WIDTH - textRenderer.getWidth(arrow)) / 2, textY, COLOR_ACCENT, false);

		int distanceX = rightAligned(context,
			Text.translatable("screen.pokebook.waypoints.distance", (int) Math.round(blip.distance())), x, y, width, COLOR_MUTED);
		int nameX = x + ARROW_WIDTH + 4;
		context.drawText(textRenderer, textRenderer.trimToWidth(blip.name().getString(), distanceX - 4 - nameX),
			nameX, textY, hovered ? COLOR_ACCENT : COLOR_TEXT, false);
	}

	@Override
	protected void onRowClicked(Radar.Blip blip, int index, double localX) {
		Navigation.navigateTo(new Navigation.FollowEntity(blip.entityId(), blip.name().getString()));
		close();
	}
}
