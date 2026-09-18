package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.call.CallState;
import io.github.lxxz.pokebook.client.call.CallFavorites;
import io.github.lxxz.pokebook.client.call.ClientCalls;
import io.github.lxxz.pokebook.network.AnswerCallPayload;
import io.github.lxxz.pokebook.network.DialCallPayload;
import io.github.lxxz.pokebook.network.HangUpCallPayload;
import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.MuteCallPayload;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	private static final int STAR_WIDTH = 12;

	/** Estrela cheia e vazia. A fonte padrão do Minecraft cobre os dois via glifo Unicode. */
	private static final String STAR_FAVORITE = "★";
	private static final String STAR_NOT_FAVORITE = "☆";

	/**
	 * Uma linha da lista: quem, se está conectado agora e se foi marcado como favorito.
	 *
	 * <p>Existe porque a lista deixou de ser só "quem está online" — favoritos continuam
	 * aparecendo desconectados, e {@link PlayerListEntry} não descreve alguém fora do
	 * mundo. {@link #skinOf} já sabia resolver a skin nos dois casos, então só a linha
	 * precisava de um tipo novo.
	 */
	private record Contact(String name, boolean online, boolean favorite) {
	}

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

	/** Igual a {@link #built}, mas para o rótulo do botão de mutar — ver {@link #tick()}. */
	private boolean builtMuted;

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
		builtMuted = ClientCalls.muted();

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
			case DIALING -> addDrawableChild(ButtonWidget.builder(
				Text.translatable("screen.pokebook.call.hang_up"),
				button -> ClientPlayNetworking.send(new HangUpCallPayload())
			).dimensions(contentX(), y, contentWidth(), BUTTON_HEIGHT).build());

			// Mutar só faz sentido com áudio de fato passando — enquanto chama, não há o
			// que tapar ainda.
			case ACTIVE -> {
				int half = (contentWidth() - BUTTON_GAP) / 2;
				addDrawableChild(ButtonWidget.builder(
					Text.translatable(ClientCalls.muted()
						? "screen.pokebook.call.unmute"
						: "screen.pokebook.call.mute"),
					button -> ClientPlayNetworking.send(new MuteCallPayload())
				).dimensions(contentX(), y, half, BUTTON_HEIGHT).build());

				addDrawableChild(ButtonWidget.builder(
					Text.translatable("screen.pokebook.call.hang_up"),
					button -> ClientPlayNetworking.send(new HangUpCallPayload())
				).dimensions(contentX() + half + BUTTON_GAP, y, contentWidth() - half - BUTTON_GAP,
					BUTTON_HEIGHT).build());
			}

			// Sem ligação não há botão: a lista inteira é a interface, e ela é desenhada
			// à mão porque rola.
			case IDLE -> {
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (ClientCalls.state() != built || ClientCalls.muted() != builtMuted) {
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
		List<Contact> roster = roster();

		if (roster.isEmpty()) {
			centered(context, Text.translatable("screen.pokebook.call.nobody"), listTop() + 16, COLOR_MUTED);
			return;
		}

		context.enableScissor(x, listTop(), x + contentWidth(), listTop() + listHeight());

		int rowY = listTop() - scroll;
		for (Contact contact : roster) {
			renderRow(context, contact, x, rowY, mouseX, mouseY);
			rowY += ROW_HEIGHT;
		}

		context.disableScissor();

		outline(context, x, listTop(), contentWidth(), listHeight(), DEBUG_AREA);
	}

	private void renderRow(DrawContext context, Contact contact, int x, int rowY, int mouseX, int mouseY) {
		outline(context, x, rowY, contentWidth(), ROW_HEIGHT, DEBUG_ROW);

		// Realce sob o mouse: numa lista sem botão, é o que diz que a linha é clicável.
		boolean hovered = mouseX >= x && mouseX < x + contentWidth()
			&& mouseY >= Math.max(rowY, listTop())
			&& mouseY < Math.min(rowY + ROW_HEIGHT, listTop() + listHeight());
		if (hovered) {
			context.fill(x, rowY, x + contentWidth(), rowY + ROW_HEIGHT, 0x30000000);
		}

		int nameY = rowY + (ROW_HEIGHT - textRenderer.fontHeight) / 2;

		Text star = Text.literal(contact.favorite() ? STAR_FAVORITE : STAR_NOT_FAVORITE);
		context.drawText(textRenderer, star, x, nameY, contact.favorite() ? COLOR_ACCENT : COLOR_MUTED, false);

		int faceX = x + STAR_WIDTH;
		int faceY = rowY + (ROW_HEIGHT - FACE_SIZE) / 2;
		PlayerSkinDrawer.draw(context, skinOf(contact.name()), faceX, faceY, FACE_SIZE);
		if (!contact.online()) {
			// Sem desenhar em tons de cinza de verdade — um véu escuro sobre o rosto já
			// diz "não está aqui agora" sem precisar tocar nos pixels da skin.
			context.fill(faceX, faceY, faceX + FACE_SIZE, faceY + FACE_SIZE, 0xA0202020);
		}

		Text name = Text.literal(contact.name());
		int nameX = faceX + FACE_SIZE + 6;
		context.drawText(textRenderer, name, nameX, nameY,
			!contact.online() ? COLOR_MUTED : hovered ? COLOR_ACCENT : COLOR_TEXT, false);
		outlineText(context, name, nameX, nameY, DEBUG_TEXT);

		Text action = Text.translatable(contact.online()
			? "screen.pokebook.call.dial"
			: "screen.pokebook.call.offline_label");
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
			case ACTIVE -> ClientCalls.muted()
				? Text.translatable("screen.pokebook.call.status.muted")
				: Text.translatable("screen.pokebook.call.status.active");
			case IDLE -> Text.empty();
		};
		centered(context, status, nameY + textRenderer.fontHeight + 4,
			built == CallState.ACTIVE && !ClientCalls.muted() ? COLOR_DONE : COLOR_MUTED);
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
		List<Contact> roster = roster();
		if (index < 0 || index >= roster.size()) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		Contact contact = roster.get(index);
		if (mouseX < x + STAR_WIDTH) {
			CallFavorites.toggle(contact.name());
			return true;
		}

		// Favorito offline continua na lista para lembrar que existe, mas não há para
		// quem ligar — o servidor recusaria do mesmo jeito, então nem manda o pacote.
		if (contact.online()) {
			ClientPlayNetworking.send(new DialCallPayload(contact.name()));
		}
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
	 * Quem dá para chamar, mais quem foi marcado como favorito mesmo sem estar aqui agora.
	 *
	 * <p>Favoritos primeiro — é a razão de existir do botão — e dentro de cada grupo por
	 * nome, para a lista não dançar entre um quadro e outro. Um favorito que também está
	 * conectado aparece uma vez só, com o rosto de verdade.
	 */
	private List<Contact> roster() {
		ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
		List<Contact> contacts = new ArrayList<>();
		Set<String> onlineNames = new HashSet<>();

		if (handler != null) {
			for (PlayerListEntry entry : handler.getPlayerList()) {
				String name = entry.getProfile().getName();
				if (!name.equals(session.nick())) {
					onlineNames.add(name);
					contacts.add(new Contact(name, true, CallFavorites.isFavorite(name)));
				}
			}
		}
		for (String favorite : CallFavorites.all()) {
			if (!favorite.equals(session.nick()) && !onlineNames.contains(favorite)) {
				contacts.add(new Contact(favorite, false, true));
			}
		}

		contacts.sort(Comparator.comparing(Contact::favorite).reversed()
			.thenComparing(Contact::name, String.CASE_INSENSITIVE_ORDER));
		return contacts;
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
