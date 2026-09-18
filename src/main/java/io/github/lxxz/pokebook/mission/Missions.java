package io.github.lxxz.pokebook.mission;

import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * As missões que existem agora.
 *
 * <p>O conteúdo vem de datapack, carregado por {@link MissionLoader} — inclusive as que
 * o próprio mod traz, que são um datapack embutido como qualquer outro. Não há lista em
 * Java: o que o mod define e o que um servidor define passam pelo mesmo caminho, então
 * não existe um caso "de dentro" que funcione diferente do caso "de fora".
 *
 * <p>O mapa é substituído inteiro a cada recarga, nunca alterado no lugar. Assim quem
 * está iterando durante um {@code /reload} termina a iteração sobre a lista antiga em vez
 * de estourar; e {@link #all()} pode ser devolvido sem cópia.
 */
public final class Missions {
	private static volatile Map<Identifier, Mission> byId = Map.of();

	private Missions() {
	}

	/** Chamado só pelo carregador, ao fim de cada recarga de datapack. */
	static void replaceAll(Map<Identifier, Mission> missions) {
		byId = Collections.unmodifiableMap(new LinkedHashMap<>(missions));
	}

	/** Na ordem de carga — é a ordem em que aparecem na interface. */
	public static Iterable<Mission> all() {
		return byId.values();
	}

	public static Mission byId(Identifier id) {
		return byId.get(id);
	}

	public static int count() {
		return byId.size();
	}
}
