package io.github.lxxz.pokebook.mission;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.lxxz.pokebook.Pokebook;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê as missões dos datapacks.
 *
 * <p>Cada arquivo em {@code data/<namespace>/pokebook/mission/<nome>.json} é uma missão,
 * e o id sai do caminho: {@code data/pokebook/pokebook/mission/three_cows.json} vira
 * {@code pokebook:three_cows}. O mod entrega as suas pelo mesmo caminho, como datapack
 * embutido — não há atalho para as de dentro.
 *
 * <p>Um arquivo inválido é registrado no log e <b>pulado</b>, sem derrubar os outros. Um
 * erro de digitação numa missão não pode apagar a lista inteira de quem está jogando.
 *
 * <p>Roda em toda recarga de datapack, inclusive no {@code /reload} com o mundo aberto —
 * é isso que permite escrever uma missão e vê-la sem reiniciar o jogo.
 */
public final class MissionLoader implements SimpleSynchronousResourceReloadListener {
	/** A pasta é {@code pokebook/mission} e não só {@code mission} para não colidir com outro mod. */
	private static final ResourceFinder FINDER = ResourceFinder.json("pokebook/mission");

	private static final Identifier LISTENER_ID = Identifier.of(Pokebook.MOD_ID, "missions");

	public static void register() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new MissionLoader());
	}

	@Override
	public Identifier getFabricId() {
		return LISTENER_ID;
	}

	@Override
	public void reload(ResourceManager manager) {
		Map<Identifier, Mission> loaded = new LinkedHashMap<>();

		// Ordenado por id: findResources não promete ordem, e a ordem aqui é a ordem em
		// que as missões aparecem na interface. Sem isto a lista embaralharia a cada carga.
		List<Map.Entry<Identifier, Resource>> files = new ArrayList<>(FINDER.findResources(manager).entrySet());
		files.sort(Map.Entry.comparingByKey());

		for (Map.Entry<Identifier, Resource> file : files) {
			Identifier missionId = FINDER.toResourceId(file.getKey());
			try (BufferedReader reader = file.getValue().getReader()) {
				JsonElement json = JsonParser.parseReader(reader);
				Mission.codec(missionId)
					.parse(JsonOps.INSTANCE, json)
					.resultOrPartial(error -> Pokebook.LOGGER.error("Missão {} inválida: {}", missionId, error))
					.ifPresent(mission -> loaded.put(missionId, mission));
			} catch (Exception e) {
				Pokebook.LOGGER.error("Falha ao ler a missão {}", missionId, e);
			}
		}

		Missions.replaceAll(loaded);
		Pokebook.LOGGER.info("{} missões carregadas.", loaded.size());
	}
}
