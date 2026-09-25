package io.github.lxxz.pokebook.client.phone;

import io.github.lxxz.pokebook.config.ServerFeatures;
import io.github.lxxz.pokebook.network.MissionEntry;
import io.github.lxxz.pokebook.network.SharedLocation;
import io.github.lxxz.pokebook.phone.PhoneData;
import io.github.lxxz.pokebook.phone.Pokephones;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O que este cliente sabe do próprio aparelho: dados, missões, o que o servidor ligou e
 * quem está mostrando a localização.
 *
 * <p><b>Eco, não autoridade</b> — o mesmo princípio de {@code ClientCalls}. Tudo aqui é o
 * último pacote que chegou; quem decide é o servidor. As telas desenham a partir daqui e
 * mandam pedidos, mas nunca mudam isto por conta própria.
 *
 * <p>Fora das telas porque é lido por quem não é tela: o HUD desenha a missão acompanhada e
 * a seta com o aparelho fechado, e a central de notificações precisa saber do "não
 * perturbe" quando um aviso chega.
 *
 * <p>Zerado ao desconectar. Sem isso, entrar noutro servidor mostraria os contatos e as
 * missões do anterior até o primeiro pacote chegar.
 */
public final class ClientPhone {
	private static PhoneData data = PhoneData.EMPTY;
	private static ServerFeatures features = ServerFeatures.ALL;
	private static List<MissionEntry> missions = List.of();
	private static List<SharedLocation> sharedLocations = List.of();

	private ClientPhone() {
	}

	public static PhoneData data() {
		return data;
	}

	public static void setData(PhoneData newData) {
		data = newData;
	}

	public static ServerFeatures features() {
		return features;
	}

	public static void setFeatures(ServerFeatures newFeatures) {
		features = newFeatures;
	}

	/**
	 * As missões, sempre as mais recentes.
	 *
	 * <p>Chegam ao entrar, ao abrir o aparelho e a cada progresso — a atualização ao vivo. É
	 * daqui que uma tela reconstrói o menu ao voltar, em vez de carregar a lista de tela em
	 * tela desde a abertura.
	 */
	public static List<MissionEntry> missions() {
		return missions;
	}

	public static void setMissions(List<MissionEntry> newMissions) {
		missions = List.copyOf(newMissions);
	}

	/** A missão que o jogador escolheu acompanhar, se ela ainda existir. */
	public static Optional<MissionEntry> trackedMission() {
		return data.trackedMission().flatMap(id ->
			missions.stream().filter(m -> m.id().equals(id)).findFirst());
	}

	public static List<SharedLocation> sharedLocations() {
		return sharedLocations;
	}

	public static void setSharedLocations(List<SharedLocation> locations) {
		sharedLocations = List.copyOf(locations);
	}

	public static Optional<SharedLocation> sharedLocationOf(UUID uuid) {
		return sharedLocations.stream().filter(l -> l.uuid().equals(uuid)).findFirst();
	}

	/**
	 * Este cliente está com um poképhone no bolso?
	 *
	 * <p>A mesma pergunta que o servidor faz antes de avisar alguém — ver
	 * {@link Pokephones}. Aqui ela decide se o HUD aparece e se o alarme toca.
	 */
	public static boolean carriesPhone() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player != null && Pokephones.carries(client.player);
	}

	public static void reset() {
		data = PhoneData.EMPTY;
		features = ServerFeatures.ALL;
		missions = List.of();
		sharedLocations = List.of();
	}
}
