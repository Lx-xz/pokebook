package io.github.lxxz.pokebook.phone;

import io.github.lxxz.pokebook.network.WeatherPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.level.ServerWorldProperties;

/**
 * A previsão do tempo: quanto falta para a chuva começar ou parar.
 *
 * <p>O servidor sabe e o cliente não — os contadores ficam nas propriedades do mundo e não
 * são sincronizados. Sempre do <b>mundo principal</b>: o Nether e o End não têm clima.
 *
 * <p>Como o vanilla conta, e é isso que a conta abaixo reproduz:
 * <ul>
 *   <li>{@code clearWeatherTime} maior que zero é tempo bom <b>forçado</b> (o
 *       {@code /weather clear} faz isso) — nada muda até ele zerar;</li>
 *   <li>senão, {@code rainTime} é quanto falta para a chuva <b>inverter</b>: se está
 *       chovendo, para; se não está, começa.</li>
 * </ul>
 * Com o ciclo de clima desligado por regra de jogo, não há previsão nenhuma.
 */
public final class Forecast {
	private Forecast() {
	}

	public static void send(ServerPlayerEntity player) {
		MinecraftServer server = player.server;
		ServerWorld overworld = server.getOverworld();
		ServerWorldProperties properties = server.getSaveProperties().getMainWorldProperties();

		boolean raining = overworld.isRaining();
		boolean thundering = overworld.isThundering();

		int ticksUntilChange;
		if (!server.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
			ticksUntilChange = -1;
		} else if (properties.getClearWeatherTime() > 0) {
			ticksUntilChange = properties.getClearWeatherTime();
		} else {
			ticksUntilChange = properties.getRainTime();
		}

		ServerPlayNetworking.send(player, new WeatherPayload(raining, thundering, ticksUntilChange));
	}
}
