package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.UpdateSettingsPayload;
import io.github.lxxz.pokebook.phone.PhoneData;
import io.github.lxxz.pokebook.phone.PhoneSettings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Ajustes do poképhone: não perturbe, quem pode ligar, papel de parede.
 *
 * <p>Cada clique manda os ajustes inteiros ao servidor, e o botão só muda quando a resposta
 * chega. A tela nunca se adianta ao servidor: o que aparece é o que está valendo — e dois
 * destes ajustes são regra que outro jogador sente, não aparência.
 *
 * <p>A <b>capa</b> não é ajuste daqui: é o próprio item, tingido na mesa de trabalho com
 * corante, como armadura de couro. A tela só lembra que dá.
 */
public class SettingsScreen extends PokebookScreenBase {
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;

	/** Os dados com que os botões foram montados, para remontar quando a resposta chegar. */
	private PhoneData built;

	public SettingsScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.settings"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	@Override
	protected void initPanel() {
		built = ClientPhone.data();
		PhoneSettings settings = built.settings();
		int y = contentTop() + 4;

		button(y, Text.translatable(settings.doNotDisturb()
				? "screen.pokebook.settings.dnd.on"
				: "screen.pokebook.settings.dnd.off"),
			"screen.pokebook.settings.dnd.tooltip",
			new PhoneSettings(!settings.doNotDisturb(), settings.callPolicy(), settings.wallpaper()));
		y += BUTTON_HEIGHT + BUTTON_GAP;

		button(y, Text.translatable("screen.pokebook.settings.calls", Text.translatable(settings.callPolicy().translationKey())),
			"screen.pokebook.settings.calls.tooltip",
			new PhoneSettings(settings.doNotDisturb(), settings.callPolicy().next(), settings.wallpaper()));
		y += BUTTON_HEIGHT + BUTTON_GAP;

		int nextWallpaper = (settings.wallpaper() + 1) % PhoneSettings.WALLPAPERS.size();
		button(y, Text.translatable("screen.pokebook.settings.wallpaper",
				settings.wallpaper() + 1, PhoneSettings.WALLPAPERS.size()),
			"screen.pokebook.settings.wallpaper.tooltip",
			new PhoneSettings(settings.doNotDisturb(), settings.callPolicy(), nextWallpaper));
	}

	private void button(int y, Text label, String tooltipKey, PhoneSettings whenClicked) {
		addDrawableChild(new TabButtonWidget(contentX(), y, contentWidth(), BUTTON_HEIGHT, label,
			b -> ClientPlayNetworking.send(new UpdateSettingsPayload(whenClicked))))
			.setTooltip(Tooltip.of(Text.translatable(tooltipKey)));
	}

	@Override
	public void tick() {
		super.tick();
		// Os dados são imutáveis e trocados inteiros a cada resposta: comparar a referência
		// basta para saber que chegou coisa nova.
		if (ClientPhone.data() != built) {
			clearAndInit();
		}
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		// A dica da capa, embaixo dos botões. Quebrada em linhas porque não cabe numa só em pé.
		int y = contentTop() + 4 + 3 * (BUTTON_HEIGHT + BUTTON_GAP) + 4;
		context.drawTextWrapped(textRenderer, Text.translatable("screen.pokebook.settings.case_hint"),
			contentX(), y, contentWidth(), COLOR_MUTED);
	}
}
