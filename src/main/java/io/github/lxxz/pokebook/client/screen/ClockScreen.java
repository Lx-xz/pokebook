package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.clock.Alarms;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.RequestWeatherPayload;
import io.github.lxxz.pokebook.network.WeatherPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.OptionalLong;

/**
 * O relógio: hora e dia do mundo, fase da lua, previsão do tempo, timer e alarme.
 *
 * <p>Num celular de verdade são abas de um app só, e aqui também — uma tela, porque cabe.
 *
 * <p>Hora, dia e lua o cliente já sabe. A previsão não: os contadores de chuva moram no
 * servidor. Ela é pedida ao abrir a tela e depois <b>extrapolada aqui</b>, descontando o tempo
 * do mundo que passou desde a resposta — pedir de novo a cada segundo seria um pacote por
 * segundo para mostrar um número que só desce.
 *
 * <p>Timer e alarme são do cliente e tocam com a tela fechada — ver {@link Alarms}. Os
 * botões são curtos de propósito (+1, +5, ✕; −, +, ●): cabem ao lado do rótulo nas duas
 * molduras, e a dica flutuante diz o que cada um faz.
 */
public class ClockScreen extends PokebookScreenBase {
	private static final int SMALL_BUTTON = 20;
	private static final int BUTTON_GAP = 2;
	private static final int ROW_GAP = 4;

	/** Um passo do alarme: uma hora do mundo. */
	private static final int ALARM_STEP = 1000;

	private WeatherPayload weather;
	/** A hora do mundo quando a previsão chegou, para descontar o que passou desde então. */
	private long weatherReceivedAt;

	/** Remontar quando o alarme mudar, para o ● acompanhar. */
	private boolean builtAlarmEnabled;

	public ClockScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.clock"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	/** Chamado quando a previsão chega do servidor. */
	public void updateWeather(WeatherPayload payload) {
		weather = payload;
		weatherReceivedAt = client != null && client.world != null ? client.world.getTime() : 0;
	}

	private int rowY(int fromBottom) {
		return contentY() + contentHeight() - SMALL_BUTTON - fromBottom * (SMALL_BUTTON + ROW_GAP);
	}

	private int buttonsX(int count) {
		return contentX() + contentWidth() - count * SMALL_BUTTON - (count - 1) * BUTTON_GAP;
	}

	@Override
	protected void initPanel() {
		if (weather == null) {
			ClientPlayNetworking.send(new RequestWeatherPayload());
		}
		builtAlarmEnabled = Alarms.alarmEnabled();

		// Timer: +1 minuto, +5 minutos, cancelar. Somar a um timer que já corre em vez de
		// recomeçar: é o que se quer quando "5 minutos" não bastou.
		int timerY = rowY(1);
		int x = buttonsX(3);
		small(x, timerY, "+1", "screen.pokebook.clock.timer.add1", () -> addToTimer(60_000));
		small(x + SMALL_BUTTON + BUTTON_GAP, timerY, "+5", "screen.pokebook.clock.timer.add5", () -> addToTimer(300_000));
		small(x + 2 * (SMALL_BUTTON + BUTTON_GAP), timerY, "✕", "screen.pokebook.clock.timer.cancel", Alarms::cancelTimer);

		// Alarme: uma hora a menos, uma a mais, ligar/desligar.
		int alarmY = rowY(0);
		small(x, alarmY, "−", "screen.pokebook.clock.alarm.earlier",
			() -> Alarms.setAlarm(Alarms.alarmEnabled(), Alarms.alarmTime() - ALARM_STEP));
		small(x + SMALL_BUTTON + BUTTON_GAP, alarmY, "+", "screen.pokebook.clock.alarm.later",
			() -> Alarms.setAlarm(Alarms.alarmEnabled(), Alarms.alarmTime() + ALARM_STEP));
		small(x + 2 * (SMALL_BUTTON + BUTTON_GAP), alarmY, Alarms.alarmEnabled() ? "●" : "○",
			Alarms.alarmEnabled() ? "screen.pokebook.clock.alarm.disable" : "screen.pokebook.clock.alarm.enable",
			() -> Alarms.setAlarm(!Alarms.alarmEnabled(), Alarms.alarmTime()));
	}

	private void addToTimer(long millis) {
		OptionalLong remaining = Alarms.timerRemaining();
		Alarms.startTimer(remaining.orElse(0) + millis);
	}

	private void small(int x, int y, String label, String tooltipKey, Runnable action) {
		ButtonWidget button = ButtonWidget.builder(Text.literal(label), b -> action.run())
			.dimensions(x, y, SMALL_BUTTON, SMALL_BUTTON)
			.tooltip(Tooltip.of(Text.translatable(tooltipKey)))
			.build();
		addDrawableChild(button);
	}

	@Override
	public void tick() {
		super.tick();
		if (Alarms.alarmEnabled() != builtAlarmEnabled) {
			clearAndInit();
		}
	}

	// ------------------------------------------------------------------ desenho

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		if (client == null || client.world == null) {
			return;
		}
		int centerX = contentX() + contentWidth() / 2;
		long timeOfDay = client.world.getTimeOfDay();

		// A hora grande, com o dobro do tamanho. A fonte tem um tamanho só; aumentar é
		// escalar a matriz, e por número inteiro para não borrar.
		String clock = Alarms.format(timeOfDay);
		int y = contentTop() + 4;
		context.getMatrices().push();
		context.getMatrices().translate(centerX, y, 0f);
		context.getMatrices().scale(2f, 2f, 1f);
		context.drawText(textRenderer, clock, -textRenderer.getWidth(clock) / 2, 0, COLOR_TEXT, false);
		context.getMatrices().pop();
		y += textRenderer.fontHeight * 2 + 4;

		long day = timeOfDay / Alarms.TICKS_PER_DAY + 1;
		Text dayLine = Text.translatable("screen.pokebook.clock.day", day,
			Text.translatable("screen.pokebook.clock.moon." + client.world.getMoonPhase()));
		drawFittedLabel(context, textRenderer, dayLine.getString(), centerX, y, contentWidth(), COLOR_MUTED);
		y += textRenderer.fontHeight + 3;

		drawFittedLabel(context, textRenderer, weatherLine().getString(), centerX, y, contentWidth(), COLOR_ACCENT);

		// Os rótulos das duas fileiras de botões, à esquerda deles.
		OptionalLong remaining = Alarms.timerRemaining();
		Text timer = remaining.isPresent()
			? Text.translatable("screen.pokebook.clock.timer", formatMillis(remaining.getAsLong()))
			: Text.translatable("screen.pokebook.clock.timer", "--:--");
		label(context, timer, rowY(1), remaining.isPresent() ? COLOR_TEXT : COLOR_MUTED);

		Text alarm = Text.translatable("screen.pokebook.clock.alarm", Alarms.format(Alarms.alarmTime()));
		label(context, alarm, rowY(0), Alarms.alarmEnabled() ? COLOR_TEXT : COLOR_MUTED);
	}

	private void label(DrawContext context, Text text, int rowY, int color) {
		int maxWidth = buttonsX(3) - 4 - contentX();
		context.drawText(textRenderer, textRenderer.trimToWidth(text.getString(), maxWidth),
			contentX(), rowY + (SMALL_BUTTON - textRenderer.fontHeight) / 2 + 1, color, false);
	}

	/**
	 * A previsão, descontando o tempo que passou desde que chegou.
	 *
	 * <p>O contador do servidor anda um por tick de mundo, então o que passou é a diferença
	 * de {@code getTime()} — o relógio absoluto, que não volta com {@code /time set}.
	 */
	private Text weatherLine() {
		if (weather == null) {
			return Text.translatable("screen.pokebook.loading");
		}
		Text now = Text.translatable(weather.thundering()
			? "screen.pokebook.clock.weather.thunder"
			: weather.raining() ? "screen.pokebook.clock.weather.rain" : "screen.pokebook.clock.weather.clear");

		if (weather.ticksUntilChange() <= 0) {
			return Text.translatable("screen.pokebook.clock.weather.no_forecast", now);
		}
		long elapsed = client != null && client.world != null ? client.world.getTime() - weatherReceivedAt : 0;
		long left = Math.max(0, weather.ticksUntilChange() - elapsed);
		String when = formatTicksRoughly(left);
		return Text.translatable(weather.raining()
			? "screen.pokebook.clock.weather.stops_in"
			: "screen.pokebook.clock.weather.starts_in", now, when);
	}

	/** Em minutos de tempo real, arredondados: a previsão não é exata a ponto de merecer segundos. */
	private static String formatTicksRoughly(long ticks) {
		long minutes = Math.max(1, Math.round(ticks / 20.0 / 60.0));
		if (minutes < 60) {
			return "~" + minutes + " min";
		}
		return "~" + Math.round(minutes / 60.0) + " h";
	}

	private static String formatMillis(long millis) {
		long seconds = (millis + 999) / 1000;
		return "%02d:%02d".formatted(seconds / 60, seconds % 60);
	}
}
