package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.call.CallState;
import io.github.lxxz.pokebook.client.call.ClientCalls;
import io.github.lxxz.pokebook.network.AnswerCallPayload;
import io.github.lxxz.pokebook.network.DialCallPayload;
import io.github.lxxz.pokebook.network.HangUpCallPayload;
import io.github.lxxz.pokebook.network.MissionEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * O telefone: para quem ligar, e o que fazer com a ligação em curso.
 *
 * <p>A tela tem <b>dois rostos</b>, e qual deles aparece é o estado da ligação, não uma
 * aba. Sem ligação é uma lista de quem está online; com ligação é uma pessoa só, grande,
 * e o botão que importa. Um telefone tocando não oferece uma lista de contatos.
 *
 * <p><b>A lista sai do próprio cliente.</b> É a mesma que a tecla Tab mostra, já
 * sincronizada pelo jogo — não há pedido ao servidor e não há tela vazia esperando
 * resposta, ao contrário da aba social. Ligar manda o apelido, e a autorização inteira
 * mora no servidor.
 *
 * <p>As linhas são desenhadas à mão, e não com widgets, porque a lista rola: widget tem
 * posição fixa e a lista muda de posição a cada quadro. Os botões de baixo, que não rolam,
 * continuam sendo widgets.
 */
public class CallScreen extends PokebookScreenBase {
	private static final int ROW_HEIGHT = 20;
	private static final int FACE_SIZE = 16;
	private static final int BIG_FACE_SIZE = 32;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;

	/** Guardadas só para reconstruir o menu ao voltar, como nas outras telas. */
	private final List<MissionEntry> missions;

	/**
	 * O estado com que os widgets atuais foram montados.
	 *
	 * <p>Existe porque a tela muda de forma sozinha: alguém atende do outro lado e o botão
	 * "desligar" precisa aparecer no lugar de "atender" sem o jogador ter tocado em nada.
	 * Comparar a cada tick é o que dispara a remontagem — ver {@link #tick()}.
	 */
	private CallState built = CallState.IDLE;

	private int scroll;

	public CallScreen(PokebookSession session, List<MissionEntry> missions) {
		super(Text.translatable("screen.pokebook.calls"), session);
		this.missions = missions;
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, missions);
	}

	@Override
	protected void initPanel() {
		built = ClientCalls.state();

		int y = contentY() + contentHeight() - BUTTON_HEIGHT;

		switch (built) {
			case RINGING -> {
				// Atender à esquerda, recusar à direita: a ação desejada é a que se alcança
				// primeiro lendo, e recusar por engano é pior do que atender por engano.
				int half = (contentWidth() - BUTTON_GAP) / 2;
				addDrawableChild(ButtonWidget.builder(
					Text.translatable("screen.pokebook.call.answer"),
					button -> ClientPlayNetworking.send(new AnswerCallPayload())
				).dimensions(contentX(), y, half, BUTTON_HEIGHT).build());

				addDrawableChild(ButtonWidget.builder(
					Text.translatable("screen.pokebook.call.decline"),
					button -> ClientPlayNetworking.send(new HangUpCallPayload())
				).dimensions(contentX() + half + BUTTON_GAP, y, contentWidth() - half - BUTTON_GAP,
					BUTTON_HEIGHT).build());
			}
			case DIALING, ACTIVE -> addDrawableChild(ButtonWidget.builder(
				Text.translatable("screen.pokebook.call.hang_up"),
				button -> ClientPlayNetworking.send(new HangUpCallPayload())
			).dimensions(contentX(), y, contentWidth(), BUTTON_HEIGHT).build());

			// Sem ligação não há botão: a lista inteira é a interface, e ela é desenhada
			// à mão porque rola.
			case IDLE -> {
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (ClientCalls.state() != built) {
			// Trocou de estado por conta do outro lado. clearAndInit() refaz os widgets
			// pelo init() da base, que repõe os botões de canto antes de chamar initPanel().
			scroll = 0;
			clearAndInit();
		}
	}

	// ------------------------------------------------------------------ desenho

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		if (built == CallState.IDLE) {
			renderRoster(context, mouseX, mouseY);
		} else {
			renderOngoing(context);
		}
	}

	/** Sem ligação: a lista de quem dá para chamar. */
	private void renderRoster(DrawContext context, int mouseX, int mouseY) {
		int x = contentX();
		List<PlayerListEntry> roster = roster();

		if (roster.isEmpty()) {
			centered(context, Text.translatable("screen.pokebook.call.nobody"), listTop() + 16, COLOR_MUTED);
			return;
		}

		context.enableScissor(x, listTop(), x + contentWidth(), listTop() + listHeight());

		int rowY = listTop() - scroll;
		for (PlayerListEntry entry : roster) {
			renderRow(context, entry, x, rowY, mouseX, mouseY);
			rowY += ROW_HEIGHT;
		}

		context.disableScissor();

		outline(context, x, listTop(), contentWidth(), listHeight(), DEBUG_AREA);
	}

	private void renderRow(DrawContext context, PlayerListEntry entry, int x, int rowY, int mouseX, int mouseY) {
		outline(context, x, rowY, contentWidth(), ROW_HEIGHT, DEBUG_ROW);

		// Realce sob o mouse: numa lista sem botão, é o que diz que a linha é clicável.
		boolean hovered = mouseX >= x && mouseX < x + contentWidth()
			&& mouseY >= Math.max(rowY, listTop())
			&& mouseY < Math.min(rowY + ROW_HEIGHT, listTop() + listHeight());
		if (hovered) {
			context.fill(x, rowY, x + contentWidth(), rowY + ROW_HEIGHT, 0x30000000);
		}

		int faceY = rowY + (ROW_HEIGHT - FACE_SIZE) / 2;
		PlayerSkinDrawer.draw(context, entry.getSkinTextures().texture(), x, faceY, FACE_SIZE);

		Text name = Text.literal(entry.getProfile().getName());
		int nameX = x + FACE_SIZE + 6;
		int nameY = rowY + (ROW_HEIGHT - textRenderer.fontHeight) / 2;
		context.drawText(textRenderer, name, nameX, nameY, hovered ? COLOR_ACCENT : COLOR_TEXT, false);
		outlineText(context, name, nameX, nameY, DEBUG_TEXT);

		Text action = Text.translatable("screen.pokebook.call.dial");
		int actionX = x + contentWidth() - textRenderer.getWidth(action);
		context.drawText(textRenderer, action, actionX, nameY, COLOR_MUTED, false);
	}

	/** Chamando, tocando ou em ligação: uma pessoa só, e o que está acontecendo com ela. */
	private void renderOngoing(DrawContext context) {
		String peer = ClientCalls.peer();
		int centerX = contentX() + contentWidth() / 2;
		int faceY = listTop() + 12;

		PlayerSkinDrawer.draw(context, skinOf(peer), centerX - BIG_FACE_SIZE / 2, faceY, BIG_FACE_SIZE);
		outline(context, centerX - BIG_FACE_SIZE / 2, faceY, BIG_FACE_SIZE, BIG_FACE_SIZE, DEBUG_AREA);

		Text name = Text.literal(peer);
		int nameY = faceY + BIG_FACE_SIZE + 6;
		context.drawText(textRenderer, name, centerX - textRenderer.getWidth(name) / 2, nameY, COLOR_TEXT, false);

		Text status = switch (built) {
			case DIALING -> Text.translatable("screen.pokebook.call.status.dialing");
			case RINGING -> Text.translatable("screen.pokebook.call.status.ringing");
			case ACTIVE -> Text.translatable("screen.pokebook.call.status.active");
			case IDLE -> Text.empty();
		};
		centered(context, status, nameY + textRenderer.fontHeight + 4,
			built == CallState.ACTIVE ? COLOR_DONE : COLOR_MUTED);
	}

	// ------------------------------------------------------------------ interação

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (built != CallState.IDLE || button != 0) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		int x = contentX();
		if (mouseX < x || mouseX >= x + contentWidth()
			|| mouseY < listTop() || mouseY >= listTop() + listHeight()) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		// O deslocamento da rolagem tem de entrar na conta: o que se vê na linha de cima
		// não é o primeiro da lista depois de rolar.
		int index = (int) ((mouseY - listTop() + scroll) / ROW_HEIGHT);
		List<PlayerListEntry> roster = roster();
		if (index < 0 || index >= roster.size()) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		ClientPlayNetworking.send(new DialCallPayload(roster.get(index).getProfile().getName()));
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (built == CallState.IDLE && maxScroll() > 0) {
			scroll = MathHelper.clamp(scroll - (int) (verticalAmount * ROW_HEIGHT / 2), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	// ------------------------------------------------------------------ medidas e dados

	private int listTop() {
		return contentTop() + 4;
	}

	private int listHeight() {
		// Com ligação em curso há um botão em baixo, e a lista não pode passar por cima.
		int bottom = contentY() + contentHeight()
			- (built == CallState.IDLE ? 0 : BUTTON_HEIGHT + BUTTON_GAP);
		return bottom - listTop();
	}

	private int maxScroll() {
		return Math.max(0, roster().size() * ROW_HEIGHT - listHeight());
	}

	/**
	 * Quem dá para chamar: todo mundo conectado, menos você.
	 *
	 * <p>Ordenado por nome para a lista não dançar entre um quadro e outro — a ordem da
	 * lista de jogadores do cliente não é estável.
	 */
	private List<PlayerListEntry> roster() {
		ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
		if (handler == null) {
			return List.of();
		}
		List<PlayerListEntry> entries = new ArrayList<>(handler.getPlayerList());
		entries.removeIf(entry -> entry.getProfile().getName().equals(session.nick()));
		entries.sort(Comparator.comparing((PlayerListEntry entry) -> entry.getProfile().getName()));
		return entries;
	}

	/**
	 * A skin de quem está do outro lado.
	 *
	 * <p>O caso de não achar é real e não precaução: durante uma ligação o outro pode
	 * desconectar, e há quadros entre isso e o servidor nos avisar.
	 */
	private Identifier skinOf(String name) {
		ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
		if (handler != null) {
			PlayerListEntry listed = handler.getPlayerListEntry(name);
			if (listed != null) {
				return listed.getSkinTextures().texture();
			}
		}
		return DefaultSkinHelper.getSkinTextures(Uuids.getOfflinePlayerUuid(name)).texture();
	}

	private void centered(DrawContext context, Text text, int y, int color) {
		context.drawText(textRenderer, text,
			contentX() + (contentWidth() - textRenderer.getWidth(text)) / 2, y, color, false);
	}
}
