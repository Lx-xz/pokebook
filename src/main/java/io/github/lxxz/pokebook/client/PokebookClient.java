package io.github.lxxz.pokebook.client;

import io.github.lxxz.pokebook.client.screen.MissionsScreen;
import io.github.lxxz.pokebook.client.screen.PokebookMenuScreen;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import io.github.lxxz.pokebook.network.OpenPokebookPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

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
	}
}
