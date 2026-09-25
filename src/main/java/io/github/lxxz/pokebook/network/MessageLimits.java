package io.github.lxxz.pokebook.network;

/**
 * Os limites das mensagens e das fotos, num lugar que o cliente e o servidor enxergam.
 *
 * <p>O cliente usa para não oferecer o que o servidor recusaria (o campo de texto para no
 * limite, a foto é reduzida antes de subir); o servidor usa para recusar de fato, porque o
 * cliente não é confiável.
 */
public final class MessageLimits {
	/** Tamanho máximo de uma mensagem, em caracteres. Cabe em poucas linhas do celular. */
	public static final int MAX_TEXT = 256;

	/** Tamanho máximo de uma foto compartilhada, já reduzida, em bytes. */
	public static final int MAX_PHOTO_BYTES = 256 * 1024;

	/**
	 * Tamanho de cada pedaço de foto no envio. O limite do pacote do cliente para o servidor é
	 * 32 767 bytes; 24 KB deixa folga para o resto do pacote.
	 */
	public static final int PHOTO_CHUNK = 24 * 1024;

	/** Maior lado de uma foto compartilhada, em pixels. Uma tela de celular não mostra mais que isso. */
	public static final int PHOTO_MAX_SIDE = 320;

	private MessageLimits() {
	}
}
