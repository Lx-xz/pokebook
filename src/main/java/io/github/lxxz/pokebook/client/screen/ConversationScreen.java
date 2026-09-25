package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.MessageLimits;
import io.github.lxxz.pokebook.network.RequestThreadPayload;
import io.github.lxxz.pokebook.network.SendMessagePayload;
import io.github.lxxz.pokebook.network.ThreadMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uma conversa: os balões, e o campo para escrever.
 *
 * <p><b>Não usa {@code ScrollListScreen}</b>, e é o único lugar: lá toda linha tem a mesma
 * altura, e aqui um balão tem tantas linhas quantas a mensagem precisar. A conta de rolagem
 * é própria, e <b>ancorada no fim</b> — rolar zero é ver a mensagem mais nova, como em todo
 * aplicativo de mensagem; chegar mensagem não tira ninguém do lugar em que estava lendo.
 *
 * <p>A tela nunca mostra uma mensagem antes de o servidor a gravar: "enviar" manda o pacote e
 * limpa o campo; a mensagem aparece quando o servidor avisa que chegou e a conversa é pedida
 * de novo. Se foi recusada, o motivo aparece acima da hotbar e o balão nunca surge.
 *
 * <p>Mensagem de foto vira um balão clicável; a foto só vem do servidor quando pedida.
 */
public class ConversationScreen extends PokebookScreenBase {
	private static final int FIELD_HEIGHT = 18;
	private static final int SEND_WIDTH = 44;
	private static final int GAP = 4;
	private static final int BUBBLE_PADDING = 3;
	private static final int BUBBLE_GAP = 3;
	/** Um balão ocupa no máximo esta fração da largura, para ficar claro de que lado está. */
	private static final float BUBBLE_MAX = 0.8f;

	private static final int MINE = 0x500D5A70;
	private static final int THEIRS = 0x28000000;

	private final UUID other;
	private final String name;

	private List<ThreadMessage> messages = List.of();
	private boolean loaded;

	/** Quanto se rolou para cima a partir do fim. Zero é a mensagem mais nova visível. */
	private int scrollFromBottom;

	private TextFieldWidget field;
	private String draft = "";

	/** Onde ficou cada balão de foto neste quadro, para o clique saber qual foi. */
	private final List<PhotoHit> photoHits = new ArrayList<>();

	private record PhotoHit(int x, int y, int width, int height, String photoId) {
	}

	public ConversationScreen(PokebookSession session, UUID other, String name) {
		super(Text.literal(name), session);
		this.other = other;
		this.name = name;
	}

	public UUID other() {
		return other;
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new MessagesScreen(session);
	}

	/** Pede a conversa de novo — ao abrir, e quando o servidor avisa que chegou mensagem. */
	public void refresh() {
		ClientPlayNetworking.send(new RequestThreadPayload(other));
	}

	/** Chamado quando a conversa chega do servidor. */
	public void update(List<ThreadMessage> updated) {
		messages = updated;
		loaded = true;
	}

	private int inputY() {
		return contentY() + contentHeight() - FIELD_HEIGHT;
	}

	private int threadTop() {
		return contentTop() + 2;
	}

	private int threadBottom() {
		return inputY() - GAP;
	}

	@Override
	protected void initPanel() {
		if (field != null) {
			draft = field.getText();
		}
		refresh();

		field = new TextFieldWidget(textRenderer, contentX(), inputY(), contentWidth() - SEND_WIDTH - GAP, FIELD_HEIGHT,
			Text.translatable("screen.pokebook.messages.field"));
		field.setMaxLength(MessageLimits.MAX_TEXT);
		field.setText(draft);
		field.setPlaceholder(Text.translatable("screen.pokebook.messages.field"));
		addDrawableChild(field);
		setInitialFocus(field);

		addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pokebook.messages.send"), button -> send())
			.dimensions(contentX() + contentWidth() - SEND_WIDTH, inputY() - 1, SEND_WIDTH, FIELD_HEIGHT + 2)
			.build());
	}

	private void send() {
		String text = field.getText().strip();
		if (text.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SendMessagePayload(other, text));
		field.setText("");
		scrollFromBottom = 0;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Enter manda, como em qualquer campo de mensagem. Sem isto seria preciso ir ao mouse
		// a cada frase.
		if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && field.isFocused()) {
			send();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	// ------------------------------------------------------------------ desenho

	/** Um balão já quebrado em linhas, com a altura que ocupa. */
	private record Bubble(ThreadMessage message, List<OrderedText> lines, int width, int height) {
	}

	private List<Bubble> layout() {
		int maxText = (int) (contentWidth() * BUBBLE_MAX) - BUBBLE_PADDING * 2;
		List<Bubble> bubbles = new ArrayList<>(messages.size());
		for (ThreadMessage message : messages) {
			Text content = message.photo().isPresent()
				? Text.translatable("screen.pokebook.messages.photo")
				: Text.literal(message.text());
			List<OrderedText> lines = textRenderer.wrapLines(content, maxText);
			int width = 0;
			for (OrderedText line : lines) {
				width = Math.max(width, textRenderer.getWidth(line));
			}
			bubbles.add(new Bubble(message, lines, width + BUBBLE_PADDING * 2,
				lines.size() * textRenderer.fontHeight + BUBBLE_PADDING * 2));
		}
		return bubbles;
	}

	private int totalHeight(List<Bubble> bubbles) {
		int total = 0;
		for (Bubble bubble : bubbles) {
			total += bubble.height() + BUBBLE_GAP;
		}
		return total;
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		photoHits.clear();
		int top = threadTop();
		int bottom = threadBottom();

		if (!loaded || messages.isEmpty()) {
			Text empty = Text.translatable(loaded ? "screen.pokebook.messages.say_hi" : "screen.pokebook.loading");
			context.drawText(textRenderer, empty,
				contentX() + (contentWidth() - textRenderer.getWidth(empty)) / 2, top + 16, COLOR_MUTED, false);
			return;
		}

		List<Bubble> bubbles = layout();
		int total = totalHeight(bubbles);
		scrollFromBottom = MathHelper.clamp(scrollFromBottom, 0, Math.max(0, total - (bottom - top)));

		context.enableScissor(contentX(), top, contentX() + contentWidth(), bottom);
		// Desenha de baixo para cima: a mais nova encosta no fundo quando não se rolou nada.
		int y = bottom + scrollFromBottom;
		for (int i = bubbles.size() - 1; i >= 0; i--) {
			Bubble bubble = bubbles.get(i);
			y -= bubble.height();
			if (y + bubble.height() >= top && y <= bottom) {
				drawBubble(context, bubble, y, mouseX, mouseY);
			}
			y -= BUBBLE_GAP;
		}
		context.disableScissor();
	}

	private void drawBubble(DrawContext context, Bubble bubble, int y, int mouseX, int mouseY) {
		boolean mine = bubble.message().mine();
		int x = mine ? contentX() + contentWidth() - bubble.width() : contentX();
		boolean photo = bubble.message().photo().isPresent();
		boolean hovered = photo && mouseX >= x && mouseX < x + bubble.width()
			&& mouseY >= Math.max(y, threadTop()) && mouseY < Math.min(y + bubble.height(), threadBottom());

		context.fill(x, y, x + bubble.width(), y + bubble.height(), mine ? MINE : THEIRS);
		int lineY = y + BUBBLE_PADDING;
		for (OrderedText line : bubble.lines()) {
			context.drawText(textRenderer, line, x + BUBBLE_PADDING, lineY,
				photo ? (hovered ? COLOR_DONE : COLOR_ACCENT) : COLOR_TEXT, false);
			lineY += textRenderer.fontHeight;
		}
		if (photo) {
			photoHits.add(new PhotoHit(x, y, bubble.width(), bubble.height(), bubble.message().photo().get()));
		}
	}

	// ------------------------------------------------------------------ interação

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && mouseY >= threadTop() && mouseY < threadBottom()) {
			for (PhotoHit hit : photoHits) {
				if (mouseX >= hit.x() && mouseX < hit.x() + hit.width()
						&& mouseY >= hit.y() && mouseY < hit.y() + hit.height()) {
					navigateTo(new SharedPhotoScreen(session, hit.photoId(), () -> new ConversationScreen(session, other, name)));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// Roda para cima sobe na conversa, para as mais velhas.
		scrollFromBottom += (int) (verticalAmount * textRenderer.fontHeight * 2);
		if (scrollFromBottom < 0) {
			scrollFromBottom = 0;
		}
		return true;
	}
}
