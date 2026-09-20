package io.github.lxxz.pokebook.client.call;

import io.github.lxxz.pokebook.Pokebook;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/**
 * Quem este cliente marcou como favorito para ligar.
 *
 * <p>É preferência de interface, não progresso de jogo: não passa pelo servidor, não é por
 * mundo, é por instalação do cliente — um arquivo de texto simples na pasta de config,
 * um apelido por linha. Comparação por apelido é <b>por conveniência</b>, não por
 * identidade: um nome reciclado por outra conta herdaria o favorito, mas é o mesmo dado
 * que a lista de quem chamar já usa (ver {@code CallService.dial}), então é consistente
 * com o resto da ligação.
 */
public final class CallFavorites {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pokebook-favorites.txt");
	private static final Logger LOGGER = Pokebook.LOGGER;

	/** {@code TreeSet} com ordem que ignora maiúsculas: "Ash" e "ash" são o mesmo contato. */
	private static final Set<String> FAVORITES = load();

	private CallFavorites() {
	}

	public static boolean isFavorite(String name) {
		return FAVORITES.contains(name);
	}

	public static void toggle(String name) {
		if (!FAVORITES.remove(name)) {
			FAVORITES.add(name);
		}
		save();
	}

	/** Todos os favoritos, para a tela juntar com quem está online agora. */
	public static Set<String> all() {
		return FAVORITES;
	}

	private static Set<String> load() {
		Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		if (Files.exists(FILE)) {
			try {
				for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
					String trimmed = line.trim();
					if (!trimmed.isEmpty()) {
						names.add(trimmed);
					}
				}
			} catch (IOException e) {
				LOGGER.warn("Não deu para ler os favoritos de ligação, começando vazio.", e);
			}
		}
		return names;
	}

	private static void save() {
		try {
			Files.write(FILE, FAVORITES, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("Não deu para salvar os favoritos de ligação.", e);
		}
	}
}
