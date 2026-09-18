package io.github.lxxz.pokebook;

import io.github.lxxz.pokebook.mission.MissionLoader;
import io.github.lxxz.pokebook.mission.MissionService;
import io.github.lxxz.pokebook.mission.MissionTracker;
import io.github.lxxz.pokebook.network.ClaimRewardPayload;
import io.github.lxxz.pokebook.network.ClosePokebookPayload;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import io.github.lxxz.pokebook.network.OpenPokebookPayload;
import io.github.lxxz.pokebook.registry.ModBlocks;
import io.github.lxxz.pokebook.registry.ModItemGroups;
import io.github.lxxz.pokebook.server.PokebookViewers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pokebook implements ModInitializer {
	public static final String MOD_ID = "pokebook";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// O bloco e o item precisam existir antes da aba do criativo, que os usa como ícone e entrada.
		ModBlocks.register();
		ModItemGroups.register();

		MissionTracker.register();
		MissionLoader.register();

		// Os codecs têm que ser registrados nos DOIS lados, senão o pacote não decodifica.
		// Este entrypoint roda tanto no cliente quanto no servidor, então é o lugar certo.
		PayloadTypeRegistry.playS2C().register(OpenPokebookPayload.ID, OpenPokebookPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MissionsUpdatePayload.ID, MissionsUpdatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ClosePokebookPayload.ID, ClosePokebookPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ClaimRewardPayload.ID, ClaimRewardPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ClosePokebookPayload.ID, (payload, context) -> {
			// Este pacote vem do cliente, que não é confiável. A validação não precisa ser
			// por distância: o conjunto de espectadores já é a autorização exata. Fechar um
			// pokébook que você não abriu não remove ninguém, e a luz não se mexe.
			PokebookViewers.close(context.player().getServerWorld(), payload.pos(), context.player());
		});

		ServerPlayNetworking.registerGlobalReceiver(ClaimRewardPayload.ID, (payload, context) ->
			MissionService.claim(context.player(), payload.missionId()));

		// Depois de um /reload a lista pode ter mudado, e quem está com o pokébook aberto
		// continuaria vendo a lista velha — inclusive missões que deixaram de existir.
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (success) {
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					ServerPlayNetworking.send(player, new MissionsUpdatePayload(MissionService.snapshot(player)));
				}
			}
		});

		// Quem desconecta nunca vai mandar o pacote de fechamento.
		ServerPlayConnectionEvents.DISCONNECT.register(
			(handler, server) -> PokebookViewers.removePlayer(handler.getPlayer()));

		LOGGER.info("Pokébook carregado.");
	}
}
