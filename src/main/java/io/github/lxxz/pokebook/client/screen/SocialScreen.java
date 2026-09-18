package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.SocialEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Quem está na frente.
 *
 * <p>É o primeiro degrau da aba social: <b>ler</b> o progresso dos outros. Nenhum sistema
 * novo — o dado já é persistido por jogador, e isto é uma consulta e uma tela. O segundo
 * degrau, mensagens entre jogadores, é um mod inteiro por si só e não está aqui.
 *
 * <p>Cada linha traz <b>rosto, nome e quantas missões a pessoa concluiu</b>, e nada mais.
 * O rosto faz a lista ser lida de relance: reconhece-se uma skin antes de se ler um nome.
 *
 * <p>A tela <b>nasce vazia</b> e se preenche quando a resposta chega. O pedido só sai ao
 * abrir esta aba: a maioria das aberturas nunca chega aqui, e mandar a lista de todo mundo
 * sempre seria pagar por algo usado às vezes.
 *
 * <p>⚠️ Só aparecem jogadores <b>conectados</b>. O progresso de quem está offline mora no
 * arquivo de save do jogador — ver {@code MissionService.socialSnapshot}.
 */
public class SocialScreen extends PokebookScreenBase {
	private static final int ROW_HEIGHT = 20;
	private static final int FACE_SIZE = 16;

	/** Guardadas só para reconstruir o menu ao voltar. */
	private final List<MissionEntry> missions;

	private List<SocialEntry> players = List.of();
	private boolean loaded;
	private int scroll;

	public SocialScreen(PokebookSession session, List<MissionEntry> missions) {
		super(Text.translatable("screen.pokebook.social"), session);
		this.missions = missions;
	}

	/** Chamado quando a resposta do servidor chega. */
	public void update(List<SocialEntry> updated) {
		this.players = new ArrayList<>(updated);
		this.loaded = true;
		this.scroll = 0;
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, missions);
	}

	private int listTop() {
		return contentTop() + 4;
	}

	private int listHeight() {
		return contentY() + contentHeight() - listTop();
	}

	private int maxScroll() {
		return Math.max(0, players.size() * ROW_HEIGHT - listHeight());
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		int x = contentX();

		if (!loaded) {
			centered(context, Text.translatable("screen.pokebook.loading"), x, listTop() + 16);
			return;
		}
		if (players.isEmpty()) {
			centered(context, Text.translatable("screen.pokebook.social.empty"), x, listTop() + 16);
			return;
		}

		context.enableScissor(x, listTop(), x + contentWidth(), listTop() + listHeight());

		int rowY = listTop() - scroll;
		for (SocialEntry entry : players) {
			renderRow(context, entry, x, rowY);
			rowY += ROW_HEIGHT;
		}

		context.disableScissor();

		outline(context, x, listTop(), contentWidth(), listHeight(), DEBUG_AREA);
	}

	private void renderRow(DrawContext context, SocialEntry entry, int x, int rowY) {
		outline(context, x, rowY, contentWidth(), ROW_HEIGHT, DEBUG_ROW);

		int faceY = rowY + (ROW_HEIGHT - FACE_SIZE) / 2;
		PlayerSkinDrawer.draw(context, skinOf(entry.name()), x, faceY, FACE_SIZE);
		outline(context, x, faceY, FACE_SIZE, FACE_SIZE, DEBUG_AREA);

		// O próprio jogador em destaque: numa lista de nomes parecidos, achar-se é a
		// primeira coisa que se faz.
		boolean self = entry.name().equals(session.nick());
		Text name = Text.literal(entry.name());
		int nameX = x + FACE_SIZE + 6;
		int nameY = rowY + (ROW_HEIGHT - textRenderer.fontHeight) / 2;
		context.drawText(textRenderer, name, nameX, nameY, self ? COLOR_ACCENT : COLOR_TEXT, false);
		outlineText(context, name, nameX, nameY, DEBUG_TEXT);

		// A contagem alinhada à direita: assim os números ficam numa coluna e dá para
		// comparar de cima a baixo sem ler cada linha inteira.
		Text count = Text.translatable("screen.pokebook.social.completed", entry.completed());
		int countX = x + contentWidth() - textRenderer.getWidth(count);
		context.drawText(textRenderer, count, countX, nameY, COLOR_MUTED, false);
		outlineText(context, count, countX, nameY, DEBUG_TEXT);
	}

	/**
	 * A skin de um jogador conectado, ou a skin padrão dele se a lista ainda não a tem.
	 *
	 * <p>A lista de jogadores é do <b>cliente</b> — é a mesma que a tecla Tab mostra. Pode
	 * não ter a entrada se o jogador acabou de sair entre a resposta do servidor e este
	 * quadro, e por isso o caso nulo existe de verdade, não por precaução.
	 */
	private Identifier skinOf(String name) {
		ClientPlayNetworkHandler handler = client == null ? null : client.getNetworkHandler();
		if (handler != null) {
			PlayerListEntry listed = handler.getPlayerListEntry(name);
			if (listed != null) {
				return listed.getSkinTextures().texture();
			}
		}
		return DefaultSkinHelper.getSkinTextures(net.minecraft.util.Uuids.getOfflinePlayerUuid(name)).texture();
	}

	private void centered(DrawContext context, Text text, int x, int y) {
		context.drawText(textRenderer, text,
			x + (contentWidth() - textRenderer.getWidth(text)) / 2, y, COLOR_MUTED, false);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (maxScroll() > 0) {
			scroll = MathHelper.clamp(scroll - (int) (verticalAmount * ROW_HEIGHT / 2), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
}
