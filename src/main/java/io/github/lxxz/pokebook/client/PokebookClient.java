package io.github.lxxz.pokebook.client;

import io.github.lxxz.pokebook.client.screen.PokebookScreen;
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
		ClientPlayNetworking.registerGlobalReceiver(OpenPokebookPayload.ID, (payload, context) -> {
			// Em 1.21.1 este handler já roda na render thread, então dá para chamar métodos
			// de cliente direto. O client.execute(...) que os tutoriais de 1.19 exigem virou
			// desnecessário.
			context.client().setScreen(new PokebookScreen(payload.pos(), payload.nick()));
		});
	}
}
