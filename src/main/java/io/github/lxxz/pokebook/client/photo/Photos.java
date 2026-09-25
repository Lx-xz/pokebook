package io.github.lxxz.pokebook.client.photo;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.client.notify.ClientNotifications;
import io.github.lxxz.pokebook.notify.NotificationKind;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
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

	public static void delete(Path photo) {
		try {
			Files.deleteIfExists(photo);
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Não deu para apagar {}.", photo, e);
		}
	}
}
