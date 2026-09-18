package io.github.lxxz.pokebook.client;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.client.render.EmissiveScreenModel;
import io.github.lxxz.pokebook.client.screen.MissionsScreen;
import io.github.lxxz.pokebook.client.screen.PokebookMenuScreen;
import io.github.lxxz.pokebook.client.screen.SocialScreen;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import io.github.lxxz.pokebook.network.OpenPokebookPayload;
import io.github.lxxz.pokebook.network.SocialUpdatePayload;
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

		// Em 1.21.1 estes handlers já rodam na render thread, então dá para chamar métodos
		// de cliente direto. O client.execute(...) que os tutoriais de 1.19 exigem virou
		// desnecessário.
		ClientPlayNetworking.registerGlobalReceiver(OpenPokebookPayload.ID, (payload, context) ->
			context.client().setScreen(
				new PokebookMenuScreen(payload.pos(), payload.nick(), payload.missions())));

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
