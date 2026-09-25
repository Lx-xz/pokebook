package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.phone.PhoneData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Notas: texto livre que o jogador guarda para si.
 *
 * <p>Guardadas no servidor, no anexo do jogador — e não num arquivo do cliente — porque são
 * <b>deste mundo</b>: "a mina de diamante é depois do rio" não faz sentido em outro save. E
 * seguem o jogador de uma máquina para outra, que é o caso do autor.
 *
 * <p>A lista mostra a primeira linha de cada uma; a mais nova fica em cima.
 */
public class NotesScreen extends ScrollListScreen<NotesScreen.Row> {
	public record Row(int index, String text) {
	}

	public NotesScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.notes"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	@Override
	protected int footerRows() {
		return 1;
	}

	/** Guardado para desligar quando o limite de notas é alcançado. */
	private ButtonWidget createButton;

	@Override
	protected void initPanel() {
		createButton = addFooterRow(0, ButtonWidget.builder(Text.translatable("screen.pokebook.notes.new"),
			button -> navigateTo(new NoteEditScreen(session, -1, "")))).get(0);
	}

	@Override
	public void tick() {
		super.tick();
		// Sem espaço, o botão de nota nova se desliga em vez de oferecer algo que o servidor
		// recusaria. Conferido a cada tick porque a lista muda quando a resposta chega.
		createButton.active = ClientPhone.data().notes().size() < PhoneData.MAX_NOTES;
	}

	@Override
	protected Text emptyText() {
		return Text.translatable("screen.pokebook.notes.empty");
	}

	@Override
	protected List<Row> rows() {
		List<String> notes = ClientPhone.data().notes();
		List<Row> rows = new ArrayList<>(notes.size());
		for (int i = 0; i < notes.size(); i++) {
			rows.add(new Row(i, notes.get(i)));
		}
		return rows;
	}

	@Override
	protected void renderRow(DrawContext context, Row row, int x, int y, int width, boolean hovered) {
		// Só a primeira linha: é o que costuma funcionar como título de uma nota.
		String first = row.text().lines().findFirst().orElse("");
		context.drawText(textRenderer, textRenderer.trimToWidth(first, width - 2), x + 1,
			y + (ROW_HEIGHT - textRenderer.fontHeight) / 2, hovered ? COLOR_ACCENT : COLOR_TEXT, false);
	}

	@Override
	protected void onRowClicked(Row row, int index, double localX) {
		navigateTo(new NoteEditScreen(session, row.index(), row.text()));
	}
}
