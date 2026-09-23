package io.github.lxxz.pokebook.client;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.client.call.ClientCalls;
import io.github.lxxz.pokebook.client.render.EmissiveScreenModel;
import io.github.lxxz.pokebook.client.screen.CallScreen;
import io.github.lxxz.pokebook.client.screen.MissionsScreen;
import io.github.lxxz.pokebook.client.screen.PokebookMenuScreen;
import io.github.lxxz.pokebook.client.screen.PokebookSession;
import io.github.lxxz.pokebook.client.screen.SocialScreen;
import io.github.lxxz.pokebook.network.CallStatePayload;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import io.github.lxxz.pokebook.network.OpenPokebookPayload;
import io.github.lxxz.pokebook.network.SocialUpdatePayload;
import io.github.lxxz.pokebook.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

		// Em 1.21.1 estes handlers já rodam na render thread, então dá para chamar métodos
		// de cliente direto. O client.execute(...) que os tutoriais de 1.19 exigem virou
		// desnecessário.
		ClientPlayNetworking.registerGlobalReceiver(OpenPokebookPayload.ID, (payload, context) -> {
			PokebookSession session = new PokebookSession(payload.pos(), payload.nick());
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
			// Só atualiza se a lista estiver de fato aberta: o resgate não deve arrastar
			// ninguém de volta para as missões se já tiver navegado para outra tela.
			if (context.client().currentScreen instanceof MissionsScreen screen) {
				screen.update(payload.missions());
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
