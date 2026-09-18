package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.call.CallService;
import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.RequestSocialPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Tela inicial do aparelho.
 *
 * <p>Existe com poucos botões de propósito. A navegação é o que define a forma dos
 * pacotes, e encaixar um menu depois significaria mexer num fluxo de dados já
 * funcionando. Com a moldura pronta, uma aba nova entra sem tocar em nada.
 *
 * <p>O título vem da sessão: o mesmo menu é a tela inicial do pokébook e do poképhone, e
 * chamar os dois de "Pokébook" seria mentira na cara do jogador.
 */
public class PokebookMenuScreen extends PokebookScreenBase {
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 6;

	private final List<MissionEntry> missions;

	public PokebookMenuScreen(PokebookSession session, List<MissionEntry> missions) {
		super(Text.translatable(session.portable()
			? "screen.pokebook.phone_title"
			: "screen.pokebook.title"), session);
		this.missions = missions;
	}

	@Override
	protected void initPanel() {
		int y = contentTop() + 8;

		addDrawableChild(ButtonWidget.builder(
			Text.translatable("screen.pokebook.missions"),
			button -> navigateTo(new MissionsScreen(session, missions))
		).dimensions(contentX(), y, contentWidth(), BUTTON_HEIGHT).build());

		y += BUTTON_HEIGHT + BUTTON_GAP;

		addDrawableChild(ButtonWidget.builder(
			Text.translatable("screen.pokebook.social"),
			button -> {
				// O pedido sai junto com a navegação, e a tela nasce vazia até a resposta
				// chegar. Pedir aqui e não ao abrir o aparelho evita mandar a lista de todo
				// mundo em aberturas que nunca chegam a esta aba.
				ClientPlayNetworking.send(new RequestSocialPayload());
				navigateTo(new SocialScreen(session, missions));
			}
		).dimensions(contentX(), y, contentWidth(), BUTTON_HEIGHT).build());

		// O botão de ligar só existe onde há como falar. Sem o Simple Voice Chat toda a
		// sinalização funcionaria e ninguém ouviria nada — um telefone mudo é pior do que
		// um telefone que não está ali. O servidor recusa de qualquer forma; esconder aqui
		// é para não oferecer o que não se pode cumprir.
		if (CallService.available()) {
			y += BUTTON_HEIGHT + BUTTON_GAP;
			addDrawableChild(ButtonWidget.builder(
				Text.translatable("screen.pokebook.calls"),
				button -> navigateTo(new CallScreen(session, missions))
			).dimensions(contentX(), y, contentWidth(), BUTTON_HEIGHT).build());
		}
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		// Sem conteúdo próprio: os botões são widgets e se desenham sozinhos.
	}
}
