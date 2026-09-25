package io.github.lxxz.pokebook.client.clock;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.client.notify.ClientNotifications;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.notify.NotificationKind;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;

/**
 * O timer e o alarme do aparelho. <b>Tudo no cliente.</b>
 *
 * <p>Nenhum dos dois é regra de jogo: são lembretes que o jogador dá a si mesmo. O servidor
 * não tem nada a conferir, e passar por ele seria um pacote a cada programação.
 *
 * <p>Dois relógios diferentes, e a diferença é a razão de existirem os dois:
 * <ul>
 *   <li><b>timer</b> conta <em>tempo real</em> — "me avise em 5 minutos", para o forno, a
 *       poção, a espera por alguém;</li>
 *   <li><b>alarme</b> olha a <em>hora do mundo</em> — "me avise ao anoitecer", que num dia de
 *       20 minutos é o lembrete que de fato importa, e que o jogador não tem como calcular
 *       de cabeça.</li>
 * </ul>
 *
 * <p>Os dois só tocam com o poképhone no inventário: sem aparelho não há o que tocar. Tocam
 * mesmo com o "não perturbe" ligado — ver {@link NotificationKind#bypassesDoNotDisturb()}.
 *
 * <p>O alarme é guardado num arquivo de config, porque "me acorde ao amanhecer" é para todo
 * dia; o timer não, porque ninguém quer um timer de cinco minutos disparando ao abrir o jogo
 * no dia seguinte.
 */
public final class Alarms {
	/** Um dia do Minecraft tem 24000 ticks, e o tick 0 é seis da manhã. */
	public static final int TICKS_PER_DAY = 24000;

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pokebook-alarm.txt");

	/** Quando o timer acaba, em milissegundos do relógio do computador. */
	private static long timerEndsAt = -1;

	private static boolean alarmEnabled;
	/** A hora do alarme, em ticks do dia (0 a 23999). */
	private static int alarmTime = 0;

	/** A última hora do mundo vista, para saber se o alarme foi cruzado entre dois ticks. */
	private static long lastTimeOfDay = -1;

	static {
		load();
	}

	private Alarms() {
	}

	// ------------------------------------------------------------------ timer

	public static void startTimer(long millis) {
		timerEndsAt = System.currentTimeMillis() + millis;
	}

	public static void cancelTimer() {
		timerEndsAt = -1;
	}

	/** Quanto falta, em milissegundos, se há timer correndo. */
	public static OptionalLong timerRemaining() {
		if (timerEndsAt < 0) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(Math.max(0, timerEndsAt - System.currentTimeMillis()));
	}

	// ------------------------------------------------------------------ alarme

	public static boolean alarmEnabled() {
		return alarmEnabled;
	}

	public static int alarmTime() {
		return alarmTime;
	}

	public static void setAlarm(boolean enabled, int time) {
		alarmEnabled = enabled;
		alarmTime = Math.floorMod(time, TICKS_PER_DAY);
		save();
	}

	// ------------------------------------------------------------------ o tique

	/** Chamado a cada tick do cliente, com ou sem tela aberta. */
	public static void tick(MinecraftClient client) {
		if (client.world == null) {
			lastTimeOfDay = -1;
			return;
		}

		if (timerEndsAt >= 0 && System.currentTimeMillis() >= timerEndsAt) {
			timerEndsAt = -1;
			if (ClientPhone.carriesPhone()) {
				ClientNotifications.push(NotificationKind.ALARM,
					Text.translatable("notification.pokebook.timer.title"),
					Text.translatable("notification.pokebook.timer.body"));
			}
		}

		long timeOfDay = Math.floorMod(client.world.getTimeOfDay(), (long) TICKS_PER_DAY);
		if (alarmEnabled && lastTimeOfDay >= 0 && crossed(lastTimeOfDay, timeOfDay, alarmTime)
				&& ClientPhone.carriesPhone()) {
			ClientNotifications.push(NotificationKind.ALARM,
				Text.translatable("notification.pokebook.alarm.title"),
				Text.translatable("notification.pokebook.alarm.body", format(alarmTime)));
		}
		lastTimeOfDay = timeOfDay;
	}

	/**
	 * A hora do alarme ficou entre a última hora vista e a de agora?
	 *
	 * <p>Cruzar, e não "ser igual": o tempo do mundo pode pular vários ticks de uma vez (um
	 * servidor lento, alguém dormindo) e passar por cima da hora exata.
	 *
	 * <p>Um salto grande para trás ou para frente — {@code /time set}, dormir até de manhã —
	 * não é contado como cruzar: só saltos curtos, senão dormir dispararia todo alarme da
	 * noite ao mesmo tempo.
	 */
	private static boolean crossed(long from, long to, int target) {
		long elapsed = Math.floorMod(to - from, (long) TICKS_PER_DAY);
		if (elapsed == 0 || elapsed > 200) {
			return false;
		}
		long untilTarget = Math.floorMod(target - from, (long) TICKS_PER_DAY);
		return untilTarget > 0 && untilTarget <= elapsed;
	}

	/**
	 * Uma hora do mundo como relógio: {@code 06:00} para o tick 0.
	 *
	 * <p>Cada hora são 1000 ticks, e o dia começa às seis — é o relógio que o próprio jogo
	 * usa no comando {@code /time}.
	 */
	public static String format(long timeOfDay) {
		long ticks = Math.floorMod(timeOfDay, (long) TICKS_PER_DAY);
		long hours = (ticks / 1000 + 6) % 24;
		long minutes = (ticks % 1000) * 60 / 1000;
		return "%02d:%02d".formatted(hours, minutes);
	}

	// ------------------------------------------------------------------ arquivo

	/** Uma linha: "ligado,hora". Formato à mão porque são dois números. */
	private static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			String[] parts = Files.readString(FILE, StandardCharsets.UTF_8).strip().split(",");
			alarmEnabled = Boolean.parseBoolean(parts[0]);
			alarmTime = Math.floorMod(Integer.parseInt(parts[1]), TICKS_PER_DAY);
		} catch (IOException | RuntimeException e) {
			Pokebook.LOGGER.warn("Não deu para ler o alarme; começando desligado.", e);
			alarmEnabled = false;
		}
	}

	private static void save() {
		try {
			Files.writeString(FILE, alarmEnabled + "," + alarmTime, StandardCharsets.UTF_8);
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Não deu para salvar o alarme.", e);
		}
	}
}
