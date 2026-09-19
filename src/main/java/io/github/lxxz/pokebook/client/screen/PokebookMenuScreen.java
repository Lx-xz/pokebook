package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.call.CallService;
import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.RequestSocialPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela inicial do aparelho: uma grade de ícones, como a tela inicial de um celular.
 *
 * <p>Substituiu botões de largura cheia empilhados. A grade não é só estética: com ícones,
 * acrescentar a quarta e a quinta função não empurra nada para fora da tela — elas caem na
 * linha seguinte sozinhas. Botões empilhados teriam esbarrado no fundo da moldura, que é o
 * mesmo problema que a lista de missões teve na quinta missão.
 *
 * <p>A grade serve aos <b>dois aparelhos</b>. O número de colunas é o mesmo; o que muda é
 * a largura disponível, e o tamanho do ícone sai dela. No pokébook os ícones ficam maiores,
 * no poképhone menores — sem nenhuma medida escrita duas vezes.
 *
 * <p>O título vem da sessão: o mesmo menu é a tela inicial do pokébook e do poképhone, e
 * chamar os dois de "Pokébook" seria mentira na cara do jogador.
 */
public class PokebookMenuScreen extends PokebookScreenBase {
	/** Três por linha. Com a largura em pé, é o máximo em que o ícone ainda se lê. */
	private static final int COLUMNS = 3;

	/** Folga entre ícones, na horizontal e na vertical. */
	private static final int TILE_GAP = 6;

	private final List<MissionEntry> missions;

	public PokebookMenuScreen(PokebookSession session, List<MissionEntry> missions) {
		super(Text.translatable(session.portable()
			? "screen.pokebook.phone_title"
			: "screen.pokebook.title"), session);
		this.missions = missions;
	}

	/**
	 * Uma função da tela inicial.
	 *
	 * <p>{@code sprite} nulo significa "ainda sem textura": o ícone cai no glifo. As
	 * texturas chegam uma a uma, e a tela não pode esperar todas para funcionar.
	 */
	private record Tile(String glyph, Identifier sprite, String labelKey, Runnable action) {
	}

	/** Uma textura de ícone pelo nome do arquivo em {@code textures/gui/sprites/}. */
	private static Identifier icon(String name) {
		return Identifier.of(Pokebook.MOD_ID, name);
	}

	@Override
	protected void initPanel() {
		List<Tile> tiles = new ArrayList<>();

		tiles.add(new Tile("◎", icon("icone_missoes"), "screen.pokebook.missions",
			() -> navigateTo(new MissionsScreen(session, missions))));

		tiles.add(new Tile("✉", icon("icone_mensagens"), "screen.pokebook.social", () -> {
			// O pedido sai junto com a navegação, e a tela nasce vazia até a resposta
			// chegar. Pedir aqui e não ao abrir o aparelho evita mandar a lista de todo
			// mundo em aberturas que nunca chegam a esta aba.
			ClientPlayNetworking.send(new RequestSocialPayload());
			navigateTo(new SocialScreen(session, missions));
		}));

		// O ícone de ligar só existe onde há como falar. Sem o Simple Voice Chat toda a
		// sinalização funcionaria e ninguém ouviria nada — um telefone mudo é pior do que
		// um telefone que não está ali. O servidor recusa de qualquer forma; esconder aqui
		// é para não oferecer o que não se pode cumprir.
		if (CallService.available()) {
			tiles.add(new Tile("☎", icon("icone_ligacoes"), "screen.pokebook.calls",
				() -> navigateTo(new CallScreen(session, missions))));
		}

		int tileSize = (contentWidth() - TILE_GAP * (COLUMNS - 1)) / COLUMNS;
		int cellHeight = IconTileWidget.heightFor(tileSize, textRenderer.fontHeight);

		for (int i = 0; i < tiles.size(); i++) {
			Tile tile = tiles.get(i);
			int column = i % COLUMNS;
			int row = i / COLUMNS;

			int x = contentX() + column * (tileSize + TILE_GAP);
			int y = contentTop() + 6 + row * (cellHeight + TILE_GAP);

			addDrawableChild(new IconTileWidget(x, y, tileSize, cellHeight,
				tile.glyph(), tile.sprite(), Text.translatable(tile.labelKey()),
				button -> tile.action().run()));
		}
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		// Sem conteúdo próprio: os ícones são widgets e se desenham sozinhos.
	}
}
