package io.github.lxxz.pokebook.client;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.client.call.ClientCalls;
import io.github.lxxz.pokebook.client.clock.Alarms;
import io.github.lxxz.pokebook.client.hud.Navigation;
import io.github.lxxz.pokebook.client.hud.PokebookHud;
import io.github.lxxz.pokebook.client.notify.ClientNotifications;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.client.photo.Photos;
import io.github.lxxz.pokebook.client.render.EmissiveScreenModel;
import io.github.lxxz.pokebook.client.screen.CallScreen;
import io.github.lxxz.pokebook.client.screen.ClockScreen;
import io.github.lxxz.pokebook.client.screen.ConversationScreen;
import io.github.lxxz.pokebook.client.screen.MessagesScreen;
import io.github.lxxz.pokebook.client.screen.SharedPhotoScreen;
import io.github.lxxz.pokebook.client.screen.MissionsScreen;
import io.github.lxxz.pokebook.client.screen.PokebookMenuScreen;
import io.github.lxxz.pokebook.client.screen.PokebookSession;
import io.github.lxxz.pokebook.client.screen.RankingScreen;
import io.github.lxxz.pokebook.client.screen.SocialScreen;
import io.github.lxxz.pokebook.network.CallStatePayload;
import io.github.lxxz.pokebook.network.ConversationsPayload;
import io.github.lxxz.pokebook.network.MessageArrivedPayload;
import io.github.lxxz.pokebook.network.PhotoDataPayload;
import io.github.lxxz.pokebook.network.ThreadPayload;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import io.github.lxxz.pokebook.network.NotificationPayload;
import io.github.lxxz.pokebook.network.OpenPokebookPayload;
import io.github.lxxz.pokebook.network.PhoneDataPayload;
import io.github.lxxz.pokebook.network.RankingPayload;
import io.github.lxxz.pokebook.network.ServerFeaturesPayload;
import io.github.lxxz.pokebook.network.SharedLocationsPayload;
import io.github.lxxz.pokebook.network.SocialUpdatePayload;
import io.github.lxxz.pokebook.network.TrackTargetPayload;
import io.github.lxxz.pokebook.network.WeatherPayload;
import io.github.lxxz.pokebook.registry.ModBlocks;
import io.github.lxxz.pokebook.registry.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Entrypoint de cliente.
 *
 * <p>Este pacote inteiro só existe no cliente físico. É ele que torna seguro mencionar
 * {@code net.minecraft.client}: o servidor dedicado nunca carrega nada daqui, então não
 * há referência a resolver e não há como quebrar.
 */
public class PokebookClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Embrulha os modelos de bloco do pokébook para que as faces de tela acesa sejam
		// desenhadas em brilho máximo. O filtro é pelo prefixo do caminho, então vale para
		// os quatro níveis de tela de uma vez; o modelo de item não entra.
		ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
			Identifier id = context.resourceId();
			if (model == null || id == null) {
				return model;
			}
			return id.getNamespace().equals(Pokebook.MOD_ID) && id.getPath().startsWith("block/pokebook")
				? new EmissiveScreenModel(model)
				: model;
		}));

		// A cor de cada pokébook colorido. O provedor devolve a cor para qualquer tintindex
		// presente no modelo; as faces sem tintindex — tela e teclado — não passam por aqui.
		// Sem provedor registrado o jogo usa branco, que é por que o pokébook comum não muda.
		ModBlocks.TINTED.forEach((nome, bloco) -> {
			int cor = ModBlocks.TINTS.get(nome);
			ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> cor, bloco);
			ColorProviderRegistry.ITEM.register((stack, tintIndex) -> cor, bloco.asItem());
		});

		// A capa do poképhone. Ao contrário do pokébook, aqui não há um item por cor: o
		// poképhone está na tag #dyeable, e o próprio vanilla o tinge na mesa de trabalho com
		// a mesma receita da armadura de couro — a cor fica no componente DYED_COLOR do item.
		// Só as faces com tintindex 0 (o chassi) recebem a cor; a tela não tem tintindex e
		// nunca passa por aqui. -1 é branco, o neutro da multiplicação: sem tinta, o aparelho
		// fica exatamente como era. É a mesma forma do provedor da armadura de couro.
		ColorProviderRegistry.ITEM.register(
			(stack, tintIndex) -> tintIndex == 0 ? DyedColorComponent.getColor(stack, -1) : -1,
			ModItems.POKEPHONE);

		// A camada de HUD: um dono só para o que o mod desenha durante o jogo.
		HudRenderCallback.EVENT.register(PokebookHud::render);

		// O que precisa de tempo passando com o aparelho fechado: o alarme que toca no bolso e
		// a foto, que é tirada alguns quadros depois de a tela fechar.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Alarms.tick(client);
			Photos.tick(client);
		});

		// Nada de um servidor pode vazar para o próximo: contatos, avisos e a seta de lá não
		// dizem nada aqui.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientPhone.reset();
			ClientNotifications.clear();
			Navigation.stop();
		});

		// Em 1.21.1 estes handlers já rodam na render thread, então dá para chamar métodos
		// de cliente direto. O client.execute(...) que os tutoriais de 1.19 exigem virou
		// desnecessário.
		ClientPlayNetworking.registerGlobalReceiver(OpenPokebookPayload.ID, (payload, context) -> {
			PokebookSession session = new PokebookSession(payload.pos(), payload.nick());
			ClientPhone.setMissions(payload.missions());
			// Com o aparelho tocando, abrir no menu seria pedir um clique a mais para
			// atender enquanto o outro espera. Quem está numa ligação abriu o aparelho por
			// causa dela.
			context.client().setScreen(ClientCalls.idle()
				? new PokebookMenuScreen(session, payload.missions())
				: new CallScreen(session, payload.missions()));
		});

		ClientPlayNetworking.registerGlobalReceiver(CallStatePayload.ID, (payload, context) ->
			// Guardado fora da tela de propósito: o estado muda quando o OUTRO faz alguma
			// coisa, e isso acontece com ou sem o aparelho aberto. A tela, se estiver
			// aberta, percebe a mudança no próprio tick e se remonta.
			ClientCalls.set(payload.state(), payload.peer(), payload.muted()));

		ClientPlayNetworking.registerGlobalReceiver(MissionsUpdatePayload.ID, (payload, context) -> {
			// Guardado sempre: é a atualização ao vivo, e o HUD lê daqui com o aparelho
			// fechado. A tela só é tocada se estiver de fato aberta: o resgate não deve
			// arrastar ninguém de volta para as missões se já tiver navegado para outra.
			ClientPhone.setMissions(payload.missions());
			if (context.client().currentScreen instanceof MissionsScreen screen) {
				screen.update(payload.missions());
			}
		});

		// Os do aparelho. Os dados vão para ClientPhone e as telas leem de lá; nenhuma tela é
		// aberta nem trocada por causa de um destes pacotes.
		ClientPlayNetworking.registerGlobalReceiver(PhoneDataPayload.ID, (payload, context) ->
			ClientPhone.setData(payload.data()));

		ClientPlayNetworking.registerGlobalReceiver(ServerFeaturesPayload.ID, (payload, context) ->
			ClientPhone.setFeatures(payload.features()));

		ClientPlayNetworking.registerGlobalReceiver(NotificationPayload.ID, (payload, context) ->
			ClientNotifications.push(payload.kind(), payload.title(), payload.body()));

		ClientPlayNetworking.registerGlobalReceiver(SharedLocationsPayload.ID, (payload, context) ->
			ClientPhone.setSharedLocations(payload.locations()));

		ClientPlayNetworking.registerGlobalReceiver(TrackTargetPayload.ID, (payload, context) -> {
			Navigation.navigateTo(new Navigation.Fixed(payload.pos(), payload.label()));
			if (context.client().player != null) {
				context.client().player.sendMessage(
					Text.translatable("message.pokebook.nav.started", payload.label()), true);
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(WeatherPayload.ID, (payload, context) -> {
			if (context.client().currentScreen instanceof ClockScreen screen) {
				screen.updateWeather(payload);
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(RankingPayload.ID, (payload, context) -> {
			if (context.client().currentScreen instanceof RankingScreen screen) {
				screen.update(payload.entries());
			}
		});

		// Mensagens. Nenhum destes abre tela: cada um só alimenta a que estiver aberta.
		ClientPlayNetworking.registerGlobalReceiver(ConversationsPayload.ID, (payload, context) -> {
			if (context.client().currentScreen instanceof MessagesScreen screen) {
				screen.update(payload.conversations());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(ThreadPayload.ID, (payload, context) -> {
			if (context.client().currentScreen instanceof ConversationScreen screen && screen.other().equals(payload.other())) {
				screen.update(payload.messages());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(MessageArrivedPayload.ID, (payload, context) -> {
			// Chegou mensagem: a tela aberta pede de novo o que mostra. Com a tela fechada, o
			// aviso já veio pela central de notificações.
			if (context.client().currentScreen instanceof ConversationScreen screen && screen.other().equals(payload.other())) {
				screen.refresh();
			} else if (context.client().currentScreen instanceof MessagesScreen screen) {
				screen.refresh();
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(PhotoDataPayload.ID, (payload, context) -> {
			if (context.client().currentScreen instanceof SharedPhotoScreen screen && screen.photoId().equals(payload.photoId())) {
				screen.show(payload.png());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(SocialUpdatePayload.ID, (payload, context) -> {
			// Pelo mesmo motivo: a resposta pode chegar depois de o jogador já ter saído
			// da aba, e nesse caso não há nada a fazer com ela.
			if (context.client().currentScreen instanceof SocialScreen screen) {
				screen.update(payload.players());
			}
		});
	}
}
