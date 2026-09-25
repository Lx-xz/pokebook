package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.call.CallService;
import io.github.lxxz.pokebook.client.notify.ClientNotifications;
import io.github.lxxz.pokebook.client.phone.ClientFlashlight;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.client.radar.Radar;
import io.github.lxxz.pokebook.config.ServerFeatures;
import io.github.lxxz.pokebook.network.FlashlightPayload;
import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.OpenPcPayload;
import io.github.lxxz.pokebook.network.RequestRankingPayload;
import io.github.lxxz.pokebook.network.RequestSocialPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela inicial do aparelho: uma grade de ícones, como a tela inicial de um celular.
 *
 * <p>Substituiu botões de largura cheia empilhados. A grade não é só estética: com ícones,
 * acrescentar a quarta e a quinta função não empurra nada para fora da tela — elas caem na
 * linha seguinte sozinhas.
 *
 * <p><b>E quando a linha seguinte também não cabe, vira outra página</b>, como num celular.
 * Com onze apps no poképhone a grade passava do fundo da moldura. Rolar não serve: os ícones
 * são widgets, e widget não convive com o que rola (ver {@code CLAUDE.md}). Página serve —
 * cada página monta os seus widgets e nada se move. Troca-se pela roda do mouse ou clicando
 * nos pontinhos de baixo, e a página é lembrada entre uma abertura e outra.
 *
 * <p>A grade serve aos <b>dois aparelhos</b>. O que muda é a largura disponível: três
 * colunas em pé, quatro deitado. Quantas linhas cabem sai da altura, e quantas páginas sai
 * das duas coisas — nenhum número de página escrito à mão.
 *
 * <p><b>Quem aparece em cada aparelho</b> segue a divisão do projeto: a estação administra, o
 * bolso comunica. Ligar, avisos, radar, fotos e ajustes são do poképhone; ranking é do
 * pokébook; o resto está nos dois. E o que o servidor desligou não aparece — ver
 * {@link ServerFeatures}.
 */
public class PokebookMenuScreen extends PokebookScreenBase {
	private static final int PORTRAIT_COLUMNS = 3;
	private static final int LANDSCAPE_COLUMNS = 4;

	/** Folga entre ícones, na horizontal e na vertical. */
	private static final int TILE_GAP = 6;

	/** Os pontinhos de página: lado, distância entre eles, e a faixa que ocupam embaixo. */
	private static final int DOT = 4;
	private static final int DOT_SPACING = 8;
	private static final int DOTS_HEIGHT = 8;

	/**
	 * A página em que o jogador estava. Estático para sobreviver a sair e voltar — a tela é
	 * construída de novo a cada navegação, e um celular que volta sempre à primeira página
	 * faz o jogador procurar o mesmo app toda vez.
	 */
	private static int lastPage;

	private final List<MissionEntry> missions;

	private List<Tile> tiles = List.of();
	private int perPage = 1;

	public PokebookMenuScreen(PokebookSession session, List<MissionEntry> missions) {
		super(Text.translatable(session.portable()
			? "screen.pokebook.phone_title"
			: "screen.pokebook.title"), session);
		this.missions = missions;
	}

	/**
	 * Uma função da tela inicial.
	 *
	 * <p>{@code sprite} nulo significa "ainda sem textura": o ícone cai no glifo. O glifo
	 * continua escrito em cada app mesmo com o desenho reusado, para o dia em que alguém
	 * preferir o glifo a um desenho repetido.
	 */
	private record Tile(String glyph, Identifier sprite, Text label, Runnable action) {
	}

	private List<Tile> buildTiles() {
		boolean phone = session.portable();
		ServerFeatures features = ClientPhone.features();
		List<Tile> list = new ArrayList<>();

		list.add(new Tile("◎", AppIcons.MISSIONS, Text.translatable("screen.pokebook.missions"),
			() -> navigateTo(new MissionsScreen(session, missions))));

		// Ligar é do aparelho de bolso, não da estação — ninguém liga de um notebook parado
		// em cima da mesa. E sem o Simple Voice Chat toda a sinalização funcionaria e ninguém
		// ouviria nada: um telefone mudo é pior do que um telefone que não está ali.
		if (phone && CallService.available()) {
			list.add(new Tile("☎", AppIcons.CALLS, Text.translatable("screen.pokebook.calls"),
				() -> navigateTo(new CallScreen(session, missions))));
		}

		// Mensagens é do bolso: comunicar é do poképhone. Com o servidor desligando, some.
		if (phone && features.messages()) {
			list.add(new Tile("✉", AppIcons.MESSAGES, Text.translatable("screen.pokebook.messages"),
				() -> navigateTo(new MessagesScreen(session))));
		}

		list.add(new Tile("☺", AppIcons.CONTACTS, Text.translatable("screen.pokebook.contacts"),
			() -> navigateTo(new ContactsScreen(session))));

		list.add(new Tile("✉", AppIcons.SOCIAL, Text.translatable("screen.pokebook.social"), () -> {
			// O pedido sai junto com a navegação, e a tela nasce vazia até a resposta
			// chegar. Pedir aqui e não ao abrir o aparelho evita mandar a lista de todo
			// mundo em aberturas que nunca chegam a esta aba.
			ClientPlayNetworking.send(new RequestSocialPayload());
			navigateTo(new SocialScreen(session, missions));
		}));

		// PC remoto: só na estação, e só se o SERVIDOR tem o Cobblemon — é lá que o PC mora.
		// A tela do PC é a dele; abrir o nosso pedido fecha esta.
		if (!phone && features.cobblemon()) {
			list.add(new Tile("▤", AppIcons.PC, Text.translatable("screen.pokebook.pc"),
				() -> session.pos().ifPresent(pos -> ClientPlayNetworking.send(new OpenPcPayload(pos)))));
		}

		// O mapa é da estação: tela grande, e o entorno de onde ela está.
		if (!phone) {
			list.add(new Tile("▦", AppIcons.MAP, Text.translatable("screen.pokebook.map"),
				() -> navigateTo(new MapScreen(session))));
		}

		if (!phone) {
			list.add(new Tile("♛", AppIcons.RANKING, Text.translatable("screen.pokebook.ranking"), () -> {
				ClientPlayNetworking.send(new RequestRankingPayload());
				navigateTo(new RankingScreen(session));
			}));
		}

		if (phone) {
			int unread = ClientNotifications.unread();
			Text label = unread > 0
				? Text.translatable("screen.pokebook.notifications.unread", unread)
				: Text.translatable("screen.pokebook.notifications");
			list.add(new Tile("!", AppIcons.NOTIFICATIONS, label,
				() -> navigateTo(new NotificationsScreen(session))));
		}

		list.add(new Tile("⚑", AppIcons.WAYPOINTS, Text.translatable("screen.pokebook.waypoints"),
			() -> navigateTo(new WaypointsScreen(session))));

		list.add(new Tile("✎", AppIcons.NOTES, Text.translatable("screen.pokebook.notes"),
			() -> navigateTo(new NotesScreen(session))));

		list.add(new Tile("◷", AppIcons.CLOCK, Text.translatable("screen.pokebook.clock"),
			() -> navigateTo(new ClockScreen(session))));

		// O radar só existe com o Cobblemon: é uma lista de Pokémon por perto, e sem ele não
		// há Pokémon. A pergunta é por id de mod, sem mencionar classe nenhuma dele.
		if (phone && features.radar() && Radar.available()) {
			list.add(new Tile("◉", AppIcons.RADAR, Text.translatable("screen.pokebook.radar"),
				() -> navigateTo(new RadarScreen(session))));
		}

		if (phone && features.photos()) {
			list.add(new Tile("▣", AppIcons.PHOTOS, Text.translatable("screen.pokebook.photos"),
				() -> navigateTo(new PhotosScreen(session))));
		}

		// A lanterna não abre tela: o ícone é o interruptor, e o rótulo diz o estado.
		if (phone && features.flashlight()) {
			boolean lit = ClientFlashlight.on();
			list.add(new Tile("☀", AppIcons.FLASHLIGHT,
				Text.translatable(lit ? "screen.pokebook.flashlight.on" : "screen.pokebook.flashlight.off"), () -> {
					ClientFlashlight.set(!lit);
					ClientPlayNetworking.send(new FlashlightPayload(!lit));
					clearAndInit();
				}));
		}

		if (phone) {
			list.add(new Tile("⚙", AppIcons.SETTINGS, Text.translatable("screen.pokebook.settings"),
				() -> navigateTo(new SettingsScreen(session))));
		}

		return list;
	}

	// ------------------------------------------------------------------ grade e páginas

	private int columns() {
		return session.portrait() ? PORTRAIT_COLUMNS : LANDSCAPE_COLUMNS;
	}

	private int gridTop() {
		return contentTop() + 6;
	}

	private int cellHeight() {
		return IconTileWidget.heightFor(textRenderer.fontHeight);
	}

	private int pageCount() {
		return Math.max(1, (tiles.size() + perPage - 1) / perPage);
	}

	@Override
	protected void initPanel() {
		tiles = buildTiles();

		int columns = columns();
		int tileSize = (contentWidth() - TILE_GAP * (columns - 1)) / columns;
		int cellHeight = cellHeight();

		// Quantas linhas cabem, já descontando a faixa dos pontinhos. Ela é descontada sempre,
		// mesmo com uma página só: senão, a conta de linhas mudaria ao aparecer uma segunda
		// página, e a primeira perderia uma linha no mesmo clique em que a segunda nasce.
		int available = contentY() + contentHeight() - gridTop() - DOTS_HEIGHT;
		int rows = Math.max(1, (available + TILE_GAP) / (cellHeight + TILE_GAP));
		perPage = columns * rows;

		lastPage = MathHelper.clamp(lastPage, 0, pageCount() - 1);
		int first = lastPage * perPage;
		int last = Math.min(tiles.size(), first + perPage);

		for (int i = first; i < last; i++) {
			Tile tile = tiles.get(i);
			int slot = i - first;
			int x = contentX() + (slot % columns) * (tileSize + TILE_GAP);
			int y = gridTop() + (slot / columns) * (cellHeight + TILE_GAP);

			addDrawableChild(new IconTileWidget(x, y, tileSize, cellHeight,
				tile.glyph(), tile.sprite(), tile.label(), button -> tile.action().run()));
		}
	}

	/** Remonta os ícones — quando um estado mostrado no rótulo muda de fora da tela. */
	public void refreshTiles() {
		clearAndInit();
	}

	private void turnTo(int page) {
		int target = MathHelper.clamp(page, 0, pageCount() - 1);
		if (target != lastPage) {
			lastPage = target;
			clearAndInit();
		}
	}

	// ------------------------------------------------------------------ pontinhos

	private int dotsY() {
		return contentY() + contentHeight() - DOTS_HEIGHT + (DOTS_HEIGHT - DOT) / 2;
	}

	private int dotX(int page) {
		int total = pageCount() * DOT_SPACING - (DOT_SPACING - DOT);
		return contentX() + (contentWidth() - total) / 2 + page * DOT_SPACING;
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		// Os ícones são widgets e se desenham sozinhos; aqui só os pontinhos, e só com mais
		// de uma página — um pontinho sozinho não diz nada.
		if (pageCount() < 2) {
			return;
		}
		for (int page = 0; page < pageCount(); page++) {
			int x = dotX(page);
			int color = page == lastPage ? COLOR_ACCENT : COLOR_MUTED;
			context.fill(x, dotsY(), x + DOT, dotsY() + DOT, color);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && pageCount() > 1 && mouseY >= dotsY() - 2 && mouseY < dotsY() + DOT + 2) {
			for (int page = 0; page < pageCount(); page++) {
				int x = dotX(page);
				// A área de clique é maior que o pontinho: quatro pixels é alvo pequeno demais.
				if (mouseX >= x - 2 && mouseX < x + DOT + 2) {
					turnTo(page);
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (pageCount() > 1 && verticalAmount != 0) {
			// Roda para baixo avança, como rolar uma lista para ver o que vem depois.
			turnTo(lastPage + (verticalAmount < 0 ? 1 : -1));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
}
