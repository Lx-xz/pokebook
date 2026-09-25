package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.RankingEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * O ranking de missões de todos os tempos — só no pokébook.
 *
 * <p>Parente da aba social, mas outra pergunta: a social diz quem está na frente entre os
 * presentes; o ranking inclui quem não entra há semanas, graças ao armazenamento de mundo
 * (ver {@code Ranking}). É coisa de estação, não de bolso: consulta-se sentado, não se
 * recebe andando.
 *
 * <p>Nasce vazia e se preenche quando a resposta chega, como a social.
 */
public class RankingScreen extends ScrollListScreen<RankingScreen.Row> {
	private static final int FACE = 16;

	/** @param position a colocação, com empate dividindo o mesmo número */
	public record Row(int position, RankingEntry entry) {
	}

	private List<Row> rows = List.of();
	private boolean loaded;

	public RankingScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.ranking"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	/** Chamado quando a resposta do servidor chega. */
	public void update(List<RankingEntry> entries) {
		List<Row> ranked = new ArrayList<>(entries.size());
		int position = 0;
		int previous = -1;
		for (int i = 0; i < entries.size(); i++) {
			RankingEntry entry = entries.get(i);
			// Empate divide a colocação: dois com 5 concluídas são os dois segundos, e o
			// seguinte é o quarto — o jeito de sempre de contar pódio.
			if (entry.completed() != previous) {
				position = i + 1;
				previous = entry.completed();
			}
			ranked.add(new Row(position, entry));
		}
		rows = ranked;
		loaded = true;
		resetScroll();
	}

	@Override
	protected Text emptyText() {
		return Text.translatable(loaded ? "screen.pokebook.ranking.empty" : "screen.pokebook.loading");
	}

	@Override
	protected List<Row> rows() {
		return rows;
	}

	@Override
	protected void renderRow(DrawContext context, Row row, int x, int y, int width, boolean hovered) {
		RankingEntry entry = row.entry();
		int textY = y + (ROW_HEIGHT - textRenderer.fontHeight) / 2;

		Text place = Text.literal(row.position() + "º");
		context.drawText(textRenderer, place, x, textY, row.position() <= 3 ? COLOR_ACCENT : COLOR_MUTED, false);

		int faceX = x + 20;
		PlayerFaces.draw(context, PlayerFaces.skin(entry.name()), faceX, y + (ROW_HEIGHT - FACE) / 2, FACE, entry.online());

		boolean self = entry.name().equals(session.nick());
		int countX = rightAligned(context, Text.translatable("screen.pokebook.social.completed", entry.completed()),
			x, y, width, COLOR_MUTED);
		int nameX = faceX + FACE + 4;
		context.drawText(textRenderer, textRenderer.trimToWidth(entry.name(), countX - 4 - nameX), nameX, textY,
			self ? COLOR_ACCENT : (entry.online() ? COLOR_TEXT : COLOR_MUTED), false);
	}

	@Override
	protected void onRowClicked(Row row, int index, double localX) {
		// Só leitura.
	}
}
