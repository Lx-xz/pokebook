package io.github.lxxz.pokebook.client.call;

import io.github.lxxz.pokebook.call.CallState;

/**
 * O que este cliente sabe sobre a própria ligação.
 *
 * <p>Vive fora das telas de propósito: o estado muda quando <b>o outro</b> faz alguma
 * coisa, e isso acontece com ou sem o aparelho aberto. Guardar dentro da tela faria o
 * estado nascer e morrer com ela, e atender uma ligação exigiria ter adivinhado a tempo de
 * abrir o poképhone antes de o pacote chegar.
 *
 * <p>Isto é <b>eco</b>, não autoridade. Quem decide quem está falando com quem é o
 * servidor; aqui só se guarda o último aviso recebido, para a tela desenhar e para o
 * aparelho saber que deve abrir já na ligação.
 *
 * <p>Estático porque há um cliente só. Tudo aqui é tocado apenas pela thread de rede do
 * cliente e pela de desenho, que em 1.21.1 são a mesma — ver o comentário em
 * {@code PokebookClient}.
 */
public final class ClientCalls {
	private static CallState state = CallState.IDLE;
	private static String peer = "";

	private ClientCalls() {
	}

	public static CallState state() {
		return state;
	}

	/** O apelido do outro lado, ou vazio se não há ligação. */
	public static String peer() {
		return peer;
	}

	public static boolean idle() {
		return state == CallState.IDLE;
	}

	public static void set(CallState newState, String newPeer) {
		state = newState;
		peer = newPeer;
	}
}
