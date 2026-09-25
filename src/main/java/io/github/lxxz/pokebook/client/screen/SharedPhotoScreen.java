package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.network.RequestPhotoPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Uma foto que alguém mandou numa conversa.
 *
 * <p>A foto não vem junto com a conversa: é pedida ao abrir esta tela. Uma conversa com
 * vinte fotos não pode custar vinte fotos para ser lida.
 *
 * <p>Mesmo cuidado da galeria local: uma textura só, destruída ao sair.
 */
public class SharedPhotoScreen extends PokebookScreenBase {
	private static final Identifier TEXTURE = Identifier.of(Pokebook.MOD_ID, "photo/shared");

	private final String photoId;
	private final Supplier<PokebookScreenBase> parent;

	private boolean loaded;
	private boolean failed;
	private int imageWidth;
	private int imageHeight;

	public SharedPhotoScreen(PokebookSession session, String photoId, Supplier<PokebookScreenBase> parent) {
		super(Text.translatable("screen.pokebook.messages.photo_title"), session);
		this.photoId = photoId;
		this.parent = parent;
	}

	public String photoId() {
		return photoId;
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return parent.get();
	}

	@Override
	protected void initPanel() {
		if (!loaded) {
			ClientPlayNetworking.send(new RequestPhotoPayload(photoId));
		}
	}

	/** Chamado quando a foto chega do servidor. */
	public void show(byte[] png) {
		if (client == null) {
			return;
		}
		try {
			NativeImage image = NativeImage.read(png);
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();
			client.getTextureManager().destroyTexture(TEXTURE);
			client.getTextureManager().registerTexture(TEXTURE, new NativeImageBackedTexture(image));
			loaded = true;
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Foto compartilhada {} ilegível.", photoId, e);
			failed = true;
		}
	}

	@Override
	public void removed() {
		if (loaded && client != null) {
			client.getTextureManager().destroyTexture(TEXTURE);
		}
		super.removed();
	}

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		int top = contentTop() + 2;
		int areaHeight = contentY() + contentHeight() - top;
		if (!loaded) {
			Text wait = Text.translatable(failed ? "message.pokebook.photos.gone" : "screen.pokebook.loading");
			context.drawText(textRenderer, wait,
				contentX() + (contentWidth() - textRenderer.getWidth(wait)) / 2, top + 16, COLOR_MUTED, false);
			return;
		}
		float scale = Math.min(contentWidth() / (float) imageWidth, areaHeight / (float) imageHeight);
		int width = Math.max(1, Math.round(imageWidth * scale));
		int height = Math.max(1, Math.round(imageHeight * scale));
		int x = contentX() + (contentWidth() - width) / 2;
		int y = top + (areaHeight - height) / 2;
		// A sobrecarga longa: tamanho na tela ANTES de u,v. Ver o CLAUDE.md.
		context.drawTexture(TEXTURE, x, y, width, height, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight);
	}
}
