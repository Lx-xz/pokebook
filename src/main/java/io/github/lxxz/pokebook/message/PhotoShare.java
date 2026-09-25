package io.github.lxxz.pokebook.message;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.config.ServerConfig;
import io.github.lxxz.pokebook.network.MessageLimits;
import io.github.lxxz.pokebook.network.PhotoDataPayload;
import io.github.lxxz.pokebook.network.PhotoUploadPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Fotos compartilhadas: recebe em pedaços, guarda no mundo, entrega a quem pode ver.
 *
 * <p><b>O caminho:</b> o cliente reduz a foto (maior lado de
 * {@link MessageLimits#PHOTO_MAX_SIDE} px), sobe em pedaços, e ela vira uma mensagem de foto
 * na conversa. Quem abre a conversa vê "foto" e a pede; o servidor só entrega a quem
 * participa de uma conversa em que ela foi mandada.
 *
 * <p><b>Guardadas no save do mundo</b>, em {@code pokebook_photos/}, uma por arquivo, com o id
 * como nome. Ficam ali para o dono do servidor poder <b>ver e apagar</b>: moderação de imagem
 * num servidor público é problema real, e o mínimo é o dono ter onde olhar. Todo envio vai
 * para o log com remetente, destinatário e id. O dono desliga tudo com
 * {@code photo_sharing: false} no config.
 *
 * <p>⚠️ <b>O id vem do cliente e vira nome de arquivo.</b> Por isso só é aceito se for um UUID
 * de verdade — sem isso, um {@code ../../} leria qualquer arquivo do servidor.
 */
public final class PhotoShare {
	/** Os oito primeiros bytes de todo PNG. Arquivo que não começa assim não é guardado. */
	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

	/** Um envio a meio caminho. */
	private static final class Upload {
		final UUID to;
		final int id;
		final byte[][] parts;
		int received;

		Upload(UUID to, int id, int total) {
			this.to = to;
			this.id = id;
			this.parts = new byte[total][];
		}
	}

	/** Um envio por jogador de cada vez. Um novo descarta o anterior, se incompleto. */
	private static final Map<UUID, Upload> UPLOADS = new HashMap<>();

	private PhotoShare() {
	}

	private static Path directory(MinecraftServer server) {
		return server.getSavePath(WorldSavePath.ROOT).resolve("pokebook_photos");
	}

	public static void onChunk(ServerPlayerEntity player, PhotoUploadPayload chunk) {
		int maxParts = (MessageLimits.MAX_PHOTO_BYTES + MessageLimits.PHOTO_CHUNK - 1) / MessageLimits.PHOTO_CHUNK;
		if (!ServerConfig.features().photoSharing()
				|| chunk.total() <= 0 || chunk.total() > maxParts
				|| chunk.index() < 0 || chunk.index() >= chunk.total()) {
			UPLOADS.remove(player.getUuid());
			return;
		}

		Upload upload = UPLOADS.get(player.getUuid());
		if (upload == null || upload.id != chunk.upload() || !upload.to.equals(chunk.to())
				|| upload.parts.length != chunk.total()) {
			// Consentimento conferido já no primeiro pedaço: não vale a pena receber 100 KB
			// para recusar no fim.
			if (!MessageService.canMessage(player, chunk.to())) {
				player.sendMessage(Text.translatable("message.pokebook.messages.not_accepting"), true);
				return;
			}
			upload = new Upload(chunk.to(), chunk.upload(), chunk.total());
			UPLOADS.put(player.getUuid(), upload);
		}
		if (upload.parts[chunk.index()] == null) {
			upload.parts[chunk.index()] = chunk.data();
			upload.received++;
		}
		if (upload.received < upload.parts.length) {
			return;
		}

		UPLOADS.remove(player.getUuid());
		finish(player, upload);
	}

	private static void finish(ServerPlayerEntity player, Upload upload) {
		ByteArrayOutputStream joined = new ByteArrayOutputStream();
		for (byte[] part : upload.parts) {
			joined.writeBytes(part);
		}
		byte[] png = joined.toByteArray();
		if (png.length > MessageLimits.MAX_PHOTO_BYTES || png.length < PNG_SIGNATURE.length
				|| !Arrays.equals(Arrays.copyOf(png, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
			player.sendMessage(Text.translatable("message.pokebook.photos.invalid"), true);
			return;
		}

		String id = UUID.randomUUID().toString();
		Path file = directory(player.server).resolve(id + ".png");
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, png);
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Não deu para guardar a foto {}.", file, e);
			player.sendMessage(Text.translatable("message.pokebook.photos.invalid"), true);
			return;
		}

		Pokebook.LOGGER.info("[Pokébook] foto {} de {} ({} bytes), guardada em {}", id,
			player.getGameProfile().getName(), png.length, file);
		if (!MessageService.send(player, upload.to, "", Optional.of(id))) {
			// Não entrou na conversa (consentimento mudou no meio, por exemplo): a foto não
			// tem quem a veja, e não fica guardada à toa.
			try {
				Files.deleteIfExists(file);
			} catch (IOException ignored) {
				// Sobra um arquivo órfão; o log acima diz qual.
			}
		}
	}

	/** Entrega uma foto a quem pediu, se ele pode vê-la. */
	public static void request(ServerPlayerEntity player, String photoId) {
		if (!isUuid(photoId) || !MessageService.canSeePhoto(player, photoId)) {
			return;
		}
		Path file = directory(player.server).resolve(photoId + ".png");
		try {
			byte[] png = Files.readAllBytes(file);
			if (png.length <= MessageLimits.MAX_PHOTO_BYTES) {
				ServerPlayNetworking.send(player, new PhotoDataPayload(photoId, png));
			}
		} catch (IOException e) {
			// Apagada pelo dono do servidor, provavelmente: a mensagem fica, a foto não.
			player.sendMessage(Text.translatable("message.pokebook.photos.gone"), true);
		}
	}

	/** É um UUID escrito do jeito canônico? Só assim vira nome de arquivo. */
	private static boolean isUuid(String text) {
		try {
			return UUID.fromString(text).toString().equals(text);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static void disconnect(ServerPlayerEntity player) {
		UPLOADS.remove(player.getUuid());
	}
}
