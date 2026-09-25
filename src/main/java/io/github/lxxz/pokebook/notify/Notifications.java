package io.github.lxxz.pokebook.notify;

import io.github.lxxz.pokebook.network.NotificationPayload;
import io.github.lxxz.pokebook.phone.Pokephones;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * A central de notificações, do lado do servidor: um jeito só de avisar alguém.
 *
 * <p>Antes dela, cada funcionalidade escolheria o seu canal — uma mensagem no chat aqui,
 * um texto acima da hotbar ali — e o jogador aprenderia um lugar diferente para cada
 * coisa. Missão concluída, ligação perdida, alguém que te salvou nos contatos, alguém que
 * começou a mostrar onde está: tudo passa por aqui e chega do mesmo jeito.
 *
 * <p><b>Só chega a quem carrega um poképhone</b> — ver {@link Pokephones}. Filtrar aqui, e
 * não no cliente, poupa o pacote para quem não teria onde recebê-lo.
 *
 * <p>O "não perturbe" <b>não</b> é conferido aqui. Ele é preferência de quem recebe, e o
 * cliente dele já sabe a própria configuração: guarda o aviso no histórico em silêncio em
 * vez de mostrar. Decidir no servidor faria o aviso sumir de vez, e uma ligação perdida
 * durante o modo avião é justamente o que se quer encontrar depois.
 */
public final class Notifications {
	private Notifications() {
	}

	public static void send(ServerPlayerEntity player, NotificationKind kind, Text title, Text body) {
		if (Pokephones.carries(player)) {
			ServerPlayNetworking.send(player, new NotificationPayload(kind, title, body));
		}
	}
}
