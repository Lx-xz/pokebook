package io.github.lxxz.pokebook.client.photo;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.client.notify.ClientNotifications;
import io.github.lxxz.pokebook.network.MessageLimits;
import io.github.lxxz.pokebook.network.PhotoUploadPayload;
import io.github.lxxz.pokebook.notify.NotificationKind;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * A câmera do poképhone: tira a foto e guarda no computador do jogador.
 *
 * <p><b>Galeria local, e só local.</b> As fotos ficam numa pasta do jogo, ao lado da pasta
 * de capturas de tela do vanilla, e nunca passam pelo servidor. Compartilhar foto é outra
 * funcionalidade, de complexidade alta — o limite de pacote do cliente para o servidor é
 * pequeno, e servidor público precisaria moderar imagem.
 *
 * <p><b>Como a foto sai limpa:</b> a tela do aparelho fecha, o HUD some (como no F1), e só
 * alguns ticks depois — com pelo menos um quadro do mundo já desenhado sem nada por cima —
 * a imagem é lida da tela. Tirar na hora sairia com o próprio celular na foto.
 *
 * <p>Gravar o PNG é feito fora da thread de desenho, no executor de entrada e saída do
 * próprio jogo — é o que a captura de tela do vanilla também faz, para o jogo não engasgar.
 */
public final class Photos {
	/** A pasta, dentro da pasta do jogo. */
	public static final Path DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("pokebook_fotos");

	private static final DateTimeFormatter NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

	/** Ticks até a captura; negativo é "nenhuma foto pedida". */
	private static int countdown = -1;
	private static boolean hudWasHidden;

	private Photos() {
	}

	/** Fecha o aparelho, esconde o HUD e marca a foto para daqui a pouco. */
	public static void requestCapture(MinecraftClient client) {
		hudWasHidden = client.options.hudHidden;
		client.options.hudHidden = true;
		client.setScreen(null);
		// Dois ticks são 100 ms: vários quadros, com folga, mesmo num computador lento.
		countdown = 2;
	}

	/** Chamado a cada tick do cliente. */
	public static void tick(MinecraftClient client) {
		if (countdown < 0) {
			return;
		}
		if (countdown-- > 0) {
			return;
		}
		countdown = -1;

		NativeImage image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
		// O HUD volta como o jogador tinha deixado: quem já estava no F1 continua nele.
		client.options.hudHidden = hudWasHidden;

		Path file = DIRECTORY.resolve(LocalDateTime.now().format(NAME) + ".png");
		Util.getIoWorkerExecutor().execute(() -> {
			try {
				Files.createDirectories(DIRECTORY);
				image.writeTo(file);
			} catch (IOException e) {
				Pokebook.LOGGER.warn("Não deu para salvar a foto em {}.", file, e);
			} finally {
				// A imagem vive em memória nativa, fora do coletor de lixo do Java: sem
				// fechar, cada foto vazaria alguns megabytes para sempre.
				image.close();
			}
		});

		ClientNotifications.push(NotificationKind.PHOTO,
			Text.translatable("notification.pokebook.photo.title"),
			Text.literal(file.getFileName().toString()));
	}

	/** As fotos, da mais nova para a mais velha. Lista vazia se a pasta ainda não existe. */
	public static List<Path> list() {
		if (!Files.isDirectory(DIRECTORY)) {
			return List.of();
		}
		try (Stream<Path> files = Files.list(DIRECTORY)) {
			return files
				.filter(path -> path.getFileName().toString().endsWith(".png"))
				.sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
				.toList();
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Não deu para listar as fotos.", e);
			return List.of();
		}
	}

	/** Identifica cada envio, para o servidor não misturar pedaços de dois envios seguidos. */
	private static int nextUpload = (int) (System.nanoTime() & 0x7FFFFFFF);

	/**
	 * Manda uma foto para um contato: reduz, codifica em PNG e sobe em pedaços.
	 *
	 * <p>Reduzida antes de sair, e não no servidor: o que viaja é o que importa, e uma captura
	 * em tela cheia passaria de um megabyte. Se mesmo reduzida ao tamanho padrão ela passar do
	 * limite — uma cena muito detalhada comprime mal —, reduz de novo, até caber.
	 *
	 * @return se a foto coube e foi mandada
	 */
	public static boolean share(Path photo, UUID to) {
		try (InputStream input = Files.newInputStream(photo); NativeImage original = NativeImage.read(input)) {
			int side = MessageLimits.PHOTO_MAX_SIDE;
			byte[] png = null;
			while (side >= 80) {
				png = downscaled(original, side);
				if (png.length <= MessageLimits.MAX_PHOTO_BYTES) {
					break;
				}
				side = side * 3 / 4;
				png = null;
			}
			if (png == null) {
				return false;
			}

			int upload = nextUpload++;
			int chunk = MessageLimits.PHOTO_CHUNK;
			int total = (png.length + chunk - 1) / chunk;
			for (int i = 0; i < total; i++) {
				byte[] part = Arrays.copyOfRange(png, i * chunk, Math.min(png.length, (i + 1) * chunk));
				ClientPlayNetworking.send(new PhotoUploadPayload(to, upload, i, total, part));
			}
			return true;
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Não deu para preparar a foto {} para envio.", photo, e);
			return false;
		}
	}

	/** A imagem reduzida para caber num quadrado de {@code side}, sem deformar, em PNG. */
	private static byte[] downscaled(NativeImage original, int side) throws IOException {
		float scale = Math.min(1f, side / (float) Math.max(original.getWidth(), original.getHeight()));
		int width = Math.max(1, Math.round(original.getWidth() * scale));
		int height = Math.max(1, Math.round(original.getHeight() * scale));
		try (NativeImage small = new NativeImage(width, height, false)) {
			original.resizeSubRectTo(0, 0, original.getWidth(), original.getHeight(), small);
			return small.getBytes();
		}
	}

	public static void delete(Path photo) {
		try {
			Files.deleteIfExists(photo);
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Não deu para apagar {}.", photo, e);
		}
	}
}
