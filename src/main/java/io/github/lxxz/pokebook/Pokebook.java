package io.github.lxxz.pokebook;

import io.github.lxxz.pokebook.battle.BattleChallenges;
import io.github.lxxz.pokebook.call.CallService;
import io.github.lxxz.pokebook.command.PokebookCommand;
import io.github.lxxz.pokebook.config.ServerConfig;
import io.github.lxxz.pokebook.integration.CobblemonIntegration;
import io.github.lxxz.pokebook.mission.MissionLoader;
import io.github.lxxz.pokebook.mission.MissionService;
import io.github.lxxz.pokebook.mission.MissionTracker;
import io.github.lxxz.pokebook.network.AnswerCallPayload;
import io.github.lxxz.pokebook.network.CallStatePayload;
import io.github.lxxz.pokebook.network.ChallengePayload;
import io.github.lxxz.pokebook.network.OpenPcPayload;
import io.github.lxxz.pokebook.network.ClaimRewardPayload;
import io.github.lxxz.pokebook.network.ClosePokebookPayload;
import io.github.lxxz.pokebook.network.ContactActionPayload;
import io.github.lxxz.pokebook.network.DialCallPayload;
import io.github.lxxz.pokebook.network.HangUpCallPayload;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import io.github.lxxz.pokebook.network.MuteCallPayload;
import io.github.lxxz.pokebook.network.NoteActionPayload;
import io.github.lxxz.pokebook.network.NotificationPayload;
import io.github.lxxz.pokebook.network.OpenPokebookPayload;
import io.github.lxxz.pokebook.network.PhoneDataPayload;
import io.github.lxxz.pokebook.network.RankingPayload;
import io.github.lxxz.pokebook.network.RequestRankingPayload;
import io.github.lxxz.pokebook.network.RequestSocialPayload;
import io.github.lxxz.pokebook.network.RequestWeatherPayload;
import io.github.lxxz.pokebook.network.ServerFeaturesPayload;
import io.github.lxxz.pokebook.network.ShareLocationPayload;
import io.github.lxxz.pokebook.network.SharedLocationsPayload;
import io.github.lxxz.pokebook.network.SocialUpdatePayload;
import io.github.lxxz.pokebook.network.TrackMissionPayload;
import io.github.lxxz.pokebook.network.TrackTargetPayload;
import io.github.lxxz.pokebook.network.UpdateSettingsPayload;
import io.github.lxxz.pokebook.network.WaypointActionPayload;
import io.github.lxxz.pokebook.network.WeatherPayload;
import io.github.lxxz.pokebook.phone.ChatLocation;
import io.github.lxxz.pokebook.phone.Forecast;
import io.github.lxxz.pokebook.phone.LocationSharing;
import io.github.lxxz.pokebook.phone.PhoneService;
import io.github.lxxz.pokebook.ranking.Ranking;
import io.github.lxxz.pokebook.registry.ModBlocks;
import io.github.lxxz.pokebook.registry.ModItemGroups;
import io.github.lxxz.pokebook.registry.ModItems;
import io.github.lxxz.pokebook.server.PokebookViewers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
		// O bloco e o item precisam existir antes da aba do criativo, que os lista.
		ModBlocks.register();
		ModItems.register();
		ModItemGroups.register();

		MissionTracker.register();
		MissionLoader.register();
		// Os dois só carregam a classe, para os anexos serem registrados agora, durante a
		// inicialização — registrar um anexo depois do mundo aberto é tarde.
		PhoneService.register();
		Ranking.register();

		// Relida a cada início de servidor; num jogo solo, cada vez que o mundo abre.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerConfig.load());

		CommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess, environment) -> PokebookCommand.register(dispatcher));

		// Os codecs têm que ser registrados nos DOIS lados, senão o pacote não decodifica.
		// Este entrypoint roda tanto no cliente quanto no servidor, então é o lugar certo.
		PayloadTypeRegistry.playS2C().register(OpenPokebookPayload.ID, OpenPokebookPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MissionsUpdatePayload.ID, MissionsUpdatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SocialUpdatePayload.ID, SocialUpdatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CallStatePayload.ID, CallStatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestSocialPayload.ID, RequestSocialPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ClosePokebookPayload.ID, ClosePokebookPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ClaimRewardPayload.ID, ClaimRewardPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DialCallPayload.ID, DialCallPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AnswerCallPayload.ID, AnswerCallPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(HangUpCallPayload.ID, HangUpCallPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MuteCallPayload.ID, MuteCallPayload.CODEC);

		// Os do aparelho: dados, avisos, localização, relógio e ranking.
		PayloadTypeRegistry.playS2C().register(PhoneDataPayload.ID, PhoneDataPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerFeaturesPayload.ID, ServerFeaturesPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NotificationPayload.ID, NotificationPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SharedLocationsPayload.ID, SharedLocationsPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TrackTargetPayload.ID, TrackTargetPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WeatherPayload.ID, WeatherPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RankingPayload.ID, RankingPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ContactActionPayload.ID, ContactActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(NoteActionPayload.ID, NoteActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(WaypointActionPayload.ID, WaypointActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UpdateSettingsPayload.ID, UpdateSettingsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TrackMissionPayload.ID, TrackMissionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ShareLocationPayload.ID, ShareLocationPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestWeatherPayload.ID, RequestWeatherPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestRankingPayload.ID, RequestRankingPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(OpenPcPayload.ID, OpenPcPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ChallengePayload.ID, ChallengePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ClosePokebookPayload.ID, (payload, context) -> {
			// Este pacote vem do cliente, que não é confiável. A validação não precisa ser
			// por distância: o conjunto de espectadores já é a autorização exata. Fechar um
			// pokébook que você não abriu não remove ninguém, e a luz não se mexe.
			PokebookViewers.close(context.player().getServerWorld(), payload.pos(), context.player());
		});

		ServerPlayNetworking.registerGlobalReceiver(ClaimRewardPayload.ID, (payload, context) ->
			MissionService.claim(context.player(), payload.missionId()));

		ServerPlayNetworking.registerGlobalReceiver(RequestSocialPayload.ID, (payload, context) ->
			ServerPlayNetworking.send(context.player(),
				new SocialUpdatePayload(MissionService.socialSnapshot(context.player().server))));

		// Os quatro da ligação. Nenhum deles confia no que veio: quem pode ligar, atender,
		// desligar ou mutar o quê é decidido inteiro dentro do CallService.
		ServerPlayNetworking.registerGlobalReceiver(DialCallPayload.ID, (payload, context) ->
			CallService.dial(context.player(), payload.target()));

		ServerPlayNetworking.registerGlobalReceiver(AnswerCallPayload.ID, (payload, context) ->
			CallService.answer(context.player()));

		ServerPlayNetworking.registerGlobalReceiver(HangUpCallPayload.ID, (payload, context) ->
			CallService.hangUp(context.player()));

		ServerPlayNetworking.registerGlobalReceiver(MuteCallPayload.ID, (payload, context) ->
			CallService.toggleMute(context.player()));

		// Os do aparelho. Mesmo princípio: nenhum confia no que veio, e todos terminam
		// devolvendo ao cliente o que de fato ficou gravado.
		ServerPlayNetworking.registerGlobalReceiver(ContactActionPayload.ID, (payload, context) ->
			PhoneService.onContactAction(context.player(), payload.action(), payload.target()));

		ServerPlayNetworking.registerGlobalReceiver(NoteActionPayload.ID, (payload, context) ->
			PhoneService.onNoteAction(context.player(), payload.index(), payload.text()));

		ServerPlayNetworking.registerGlobalReceiver(WaypointActionPayload.ID, (payload, context) ->
			PhoneService.onWaypointAction(context.player(), payload.index(), payload.waypoint()));

		ServerPlayNetworking.registerGlobalReceiver(UpdateSettingsPayload.ID, (payload, context) ->
			PhoneService.updateSettings(context.player(), payload.settings()));

		ServerPlayNetworking.registerGlobalReceiver(TrackMissionPayload.ID, (payload, context) ->
			PhoneService.trackMission(context.player(), payload.missionId()));

		ServerPlayNetworking.registerGlobalReceiver(ShareLocationPayload.ID, (payload, context) ->
			ChatLocation.share(context.player(), payload.waypoint()));

		ServerPlayNetworking.registerGlobalReceiver(RequestWeatherPayload.ID, (payload, context) ->
			Forecast.send(context.player()));

		ServerPlayNetworking.registerGlobalReceiver(RequestRankingPayload.ID, (payload, context) ->
			ServerPlayNetworking.send(context.player(),
				new RankingPayload(Ranking.snapshot(context.player().server))));

		// O telefone precisa de tempo passando: é o que repete o aviso acima da hotbar e o
		// que desiste de quem não atende. Sai barato — sem ligação nenhuma, não faz nada.
		ServerTickEvents.END_SERVER_TICK.register(CallService::tick);
		ServerTickEvents.END_SERVER_TICK.register(LocationSharing::tick);
		ServerTickEvents.END_SERVER_TICK.register(BattleChallenges::tick);

		ServerPlayNetworking.registerGlobalReceiver(ChallengePayload.ID, (payload, context) ->
			BattleChallenges.challenge(context.player(), payload.target()));

		// Depois de um /reload a lista pode ter mudado, e quem está com o pokébook aberto
		// continuaria vendo a lista velha — inclusive missões que deixaram de existir.
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (success) {
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					ServerPlayNetworking.send(player, new MissionsUpdatePayload(MissionService.snapshot(player)));
				}
			}
		});

		// Quem entra pode ter chegado depois de a última transição de ligação ter passado.
		// Mandar o estado aqui é o que faz o cliente nascer sabendo — e IDLE é o caso
		// normal, então isto é quase sempre um pacote de nada.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			CallService.sendState(player);
			ServerPlayNetworking.send(player, new ServerFeaturesPayload(ServerConfig.features()));
			// As missões vão já na entrada, e não só ao abrir o pokébook: a missão
			// acompanhada no HUD precisa aparecer antes de o jogador abrir qualquer coisa.
			ServerPlayNetworking.send(player, new MissionsUpdatePayload(MissionService.snapshot(player)));
			PhoneService.onJoin(player);
			Ranking.update(player);
		});

		// Quem desconecta nunca vai mandar o pacote de fechamento — nem desligar o telefone.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			PokebookViewers.removePlayer(handler.getPlayer());
			CallService.disconnect(handler.getPlayer());
			LocationSharing.disconnect(handler.getPlayer());
			ChatLocation.disconnect(handler.getPlayer());
			BattleChallenges.disconnect(handler.getPlayer());
		});

		// Por último, depois de todos os tipos de pacote registrados: a integração registra
		// receptores próprios, e a Fabric exige o tipo registrado antes do receptor.
		// A classe só é tocada depois desta checagem. Ver o comentário dentro dela: a
		// fronteira é a classe, não este if -- o if sozinho não salvaria nada.
		if (FabricLoader.getInstance().isModLoaded("cobblemon")) {
			CobblemonIntegration.register();
		}

		LOGGER.info("Pokébook carregado.");
	}
}
