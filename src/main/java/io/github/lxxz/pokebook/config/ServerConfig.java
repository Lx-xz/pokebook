package io.github.lxxz.pokebook.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.lxxz.pokebook.Pokebook;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * O arquivo {@code config/pokebook-server.json}.
 *
 * <p>JSON à mão com o Gson, que já vem dentro do Minecraft — nenhuma dependência nova
 * para ler quatro booleanos. Se o arquivo não existe, ele é criado com tudo ligado, para o
 * dono do servidor achar as chaves sem precisar ler documentação.
 *
 * <p><b>Chave ausente vale "ligado".</b> Um arquivo escrito por uma versão antiga, sem as
 * chaves que vieram depois, não desliga nada sem querer. E arquivo ilegível também não
 * derruba o servidor: loga e segue com o padrão, porque um pokébook sem configuração é um
 * defeito pequeno e um servidor que não sobe não é.
 *
 * <p>Relido a cada início de servidor. Em um jogo solo, isso é cada vez que o mundo abre.
 */
public final class ServerConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pokebook-server.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static ServerFeatures features = ServerFeatures.ALL;

	private ServerConfig() {
	}

	public static ServerFeatures features() {
		return features;
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			features = withCobblemon(ServerFeatures.ALL);
			write(features);
			return;
		}
		try {
			JsonObject json = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
			features = new ServerFeatures(
				flag(json, "location_sharing"),
				flag(json, "chat_location"),
				flag(json, "radar"),
				flag(json, "photos"),
				flag(json, "messages"),
				flag(json, "photo_sharing"),
				flag(json, "flashlight"),
				cobblemonPresent()
			);
			// Um arquivo de versão antiga ganha as chaves novas, para o dono as encontrar.
			write(features);
		} catch (IOException | RuntimeException e) {
			// RuntimeException pega JSON malformado e raiz que não é objeto, que o Gson
			// lança como exceções não verificadas.
			Pokebook.LOGGER.warn("Não deu para ler {}; seguindo com tudo ligado.", FILE, e);
			features = withCobblemon(ServerFeatures.ALL);
		}
	}

	/** Se o servidor tem o Cobblemon. Não é chave do arquivo: é fato da instalação. */
	private static boolean cobblemonPresent() {
		return FabricLoader.getInstance().isModLoaded("cobblemon");
	}

	private static ServerFeatures withCobblemon(ServerFeatures base) {
		return new ServerFeatures(base.locationSharing(), base.chatLocation(), base.radar(), base.photos(),
			base.messages(), base.photoSharing(), base.flashlight(), cobblemonPresent());
	}

	private static boolean flag(JsonObject json, String key) {
		JsonElement value = json.get(key);
		return value == null || !value.isJsonPrimitive() || value.getAsBoolean();
	}

	private static void write(ServerFeatures current) {
		JsonObject json = new JsonObject();
		json.addProperty("location_sharing", current.locationSharing());
		json.addProperty("chat_location", current.chatLocation());
		json.addProperty("radar", current.radar());
		json.addProperty("photos", current.photos());
		json.addProperty("messages", current.messages());
		json.addProperty("photo_sharing", current.photoSharing());
		json.addProperty("flashlight", current.flashlight());
		try {
			Files.writeString(FILE, GSON.toJson(json), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Não deu para criar {}.", FILE, e);
		}
	}
}
