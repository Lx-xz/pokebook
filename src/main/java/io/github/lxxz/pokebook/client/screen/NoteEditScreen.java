package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.NoteActionPayload;
import io.github.lxxz.pokebook.phone.PhoneData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;

import java.util.Optional;

/**
 * Uma nota aberta para escrever.
 *
 * <p>O campo é o {@link EditBoxWidget} do vanilla, o de várias linhas que o jogo usa no
 * livro e pena. Ele quebra linha, rola sozinho e aceita colar — escrever um editor de
 * texto não é o que este mod está aqui para fazer.
 *
 * <p>Salvar manda e volta para a lista; o que aparece lá é o que o servidor gravou. Uma nota
 * esvaziada e salva é apagada, ver {@code PhoneService.onNoteAction}.
 */
public class NoteEditScreen extends PokebookScreenBase {
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;
	private static final long CONFIRM_MS = 3000;

	private final int index;
	private String text;

	private EditBoxWidget editor;
	private long deleteArmedAt = -1;

	public NoteEditScreen(PokebookSession session, int index, String text) {
		super(Text.translatable(index < 0 ? "screen.pokebook.notes.new" : "screen.pokebook.notes.edit"), session);
		this.index = index;
		this.text = text;
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new NotesScreen(session);
	}

	@Override
	protected void initPanel() {
		// Guarda o que já foi escrito antes de remontar — remontar recria o campo vazio.
		if (editor != null) {
			text = editor.getText();
		}

		int buttonsY = contentY() + contentHeight() - BUTTON_HEIGHT;
		int editorTop = contentTop() + 2;
		editor = new EditBoxWidget(textRenderer, contentX(), editorTop, contentWidth(),
			buttonsY - BUTTON_GAP - editorTop,
			Text.translatable("screen.pokebook.notes.placeholder"), Text.translatable("screen.pokebook.notes"));
		editor.setMaxLength(PhoneData.MAX_NOTE_LENGTH);
		editor.setText(text);
		addDrawableChild(editor);
		setInitialFocus(editor);

		boolean existing = index >= 0;
		int half = (contentWidth() - BUTTON_GAP) / 2;
		int saveWidth = existing ? half : contentWidth();

		addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pokebook.save"), button -> {
			ClientPlayNetworking.send(new NoteActionPayload(index, Optional.of(editor.getText())));
			navigateTo(parentScreen());
		}).dimensions(contentX(), buttonsY, saveWidth, BUTTON_HEIGHT).build());

		if (existing) {
			boolean armed = deleteArmedAt >= 0;
			addDrawableChild(ButtonWidget.builder(
				Text.translatable(armed ? "screen.pokebook.delete_confirm" : "screen.pokebook.delete"), button -> {
					// Dois cliques: apagar não tem volta.
					if (deleteArmedAt >= 0 && System.currentTimeMillis() - deleteArmedAt < CONFIRM_MS) {
						ClientPlayNetworking.send(new NoteActionPayload(index, Optional.empty()));
						navigateTo(parentScreen());
						return;
					}
					deleteArmedAt = System.currentTimeMillis();
					clearAndInit();
				}).dimensions(contentX() + half + BUTTON_GAP, buttonsY, contentWidth() - half - BUTTON_GAP, BUTTON_HEIGHT)
				.build());
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (deleteArmedAt >= 0 && System.currentTimeMillis() - deleteArmedAt >= CONFIRM_MS) {
			deleteArmedAt = -1;
			clearAndInit();
		}
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		// O campo e os botões são widgets e se desenham sozinhos.
	}
}
