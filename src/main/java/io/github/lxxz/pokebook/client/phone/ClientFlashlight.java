package io.github.lxxz.pokebook.client.phone;

/**
 * Se a lanterna está acesa, do jeito que o servidor disse por último.
 *
 * <p>Ao tocar no ícone o estado muda na hora, para o rótulo não esperar a rede; a resposta do
 * servidor corrige se ele recusou. Ela também chega quando a lanterna se apaga sozinha — o
 * poképhone saiu do inventário, o jogador morreu.
 */
public final class ClientFlashlight {
	private static boolean on;

	private ClientFlashlight() {
	}

	public static boolean on() {
		return on;
	}

	public static void set(boolean value) {
		on = value;
	}
}
