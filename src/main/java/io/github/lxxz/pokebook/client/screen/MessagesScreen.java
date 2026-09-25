package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.ConversationSummary;
import io.github.lxxz.pokebook.network.RequestConversationsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Mensagens: a lista de conversas, da mais recente para a mais antiga.
 *
 * <p>Cada linha traz o rosto, o nome, o começo da última mensagem e, se houver, quantas
 * chegaram sem ser lidas. A lista nasce vazia e se preenche quando a resposta chega, como a
 * aba social; e se refaz sozinha quando chega mensagem com a tela aberta.
 */
public class MessagesScreen extends ScrollListScreen<ConversationSummary> {
	private static final int FACE = 16;

	private List<ConversationSummary> conversations = List.of();
	private boolean loaded;

	public MessagesScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.messages"), session);
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
		refresh();
		addFooterRow(0, ButtonWidget.builder(Text.translatable("screen.pokebook.messages.new"), button ->
			navigateTo(new ContactPickerScreen(session, Text.translatable("screen.pokebook.messages.new"),
				() -> new MessagesScreen(session),
				(picker, contact) -> picker.go(new ConversationScreen(session, contact.uuid(), contact.name()))))));
	}

	/** Pede a lista de novo — ao abrir, e quando chega mensagem com a tela aberta. */
	public void refresh() {
		ClientPlayNetworking.send(new RequestConversationsPayload());
	}

	/** Chamado quando a resposta do servidor chega. */
	public void update(List<ConversationSummary> updated) {
		conversations = updated;
		loaded = true;
	}

	@Override
	protected Text emptyText() {
		return Text.translatable(loaded ? "screen.pokebook.messages.empty" : "screen.pokebook.loading");
	}

	@Override
	protected List<ConversationSummary> rows() {
		return conversations;
	}

	@Override
	protected void renderRow(DrawContext context, ConversationSummary conversation, int x, int y, int width, boolean hovered) {
		PlayerFaces.draw(context, PlayerFaces.skin(conversation.other()), x, y + (ROW_HEIGHT - FACE) / 2, FACE,
			PlayerFaces.isOnline(conversation.other()));

		int right = x + width;
		if (conversation.unread() > 0) {
			Text badge = Text.literal(String.valueOf(conversation.unread()));
			right = rightAligned(context, badge, x, y, width, COLOR_DONE) - 4;
		}

		// Nome e começo da última mensagem na mesma linha: "Ash: vem ver isso". A linha é
		// estreita no celular, e duas linhas por conversa dobrariam a altura da lista.
		int textX = x + FACE + 4;
		String line = conversation.name() + ": " + conversation.preview();
		context.drawText(textRenderer, textRenderer.trimToWidth(line, right - textX),
			textX, y + (ROW_HEIGHT - textRenderer.fontHeight) / 2,
			conversation.unread() > 0 ? COLOR_TEXT : (hovered ? COLOR_ACCENT : COLOR_MUTED), false);
	}

	@Override
	protected void onRowClicked(ConversationSummary conversation, int index, double localX) {
		navigateTo(new ConversationScreen(session, conversation.other(), conversation.name()));
	}
}
