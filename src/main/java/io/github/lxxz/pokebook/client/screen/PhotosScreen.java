package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.client.photo.Photos;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Fotos: a câmera e a galeria no mesmo app, como num celular.
 *
 * <p><b>Uma foto por vez na tela, e uma só carregada.</b> Uma captura em tela cheia ocupa
 * uns 8 MB de memória de vídeo; uma grade de miniaturas carregaria todas de uma vez. Aqui
 * só a que está sendo vista existe como textura, e ela é destruída ao trocar de foto e ao
 * sair da tela.
 *
 * <p>Ler o PNG do disco é feito na hora, na thread de desenho — um engasgo curto ao trocar de
 * foto. Se incomodar, o caminho é ler num executor e trocar a textura quando terminar.
 */
public class PhotosScreen extends PokebookScreenBase {
	private static final Identifier TEXTURE = Identifier.of(Pokebook.MOD_ID, "photo/current");
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;
	private static final long CONFIRM_MS = 3000;

	/**
	 * Qual foto estava aberta. Estático para voltar a ela depois de tirar uma foto nova —
	 * que fecha a tela — ou de sair e voltar.
	 */
	private static int lastIndex;

	private List<Path> photos = List.of();
	private int loadedIndex = -1;
	private int imageWidth;
	private int imageHeight;
	private long deleteArmedAt = -1;

	public PhotosScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.photos"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	private int buttonsY(int fromBottom) {
		return contentY() + contentHeight() - BUTTON_HEIGHT - fromBottom * (BUTTON_HEIGHT + BUTTON_GAP);
	}

	@Override
	protected void initPanel() {
		photos = Photos.list();
		lastIndex = photos.isEmpty() ? 0 : MathHelper.clamp(lastIndex, 0, photos.size() - 1);

		addDrawableChild(ButtonWidget.builder(Text.translatable("screen.pokebook.photos.take"), button -> {
			// A foto nova entra no topo da lista, e é ela que o jogador quer ver ao voltar.
			lastIndex = 0;
			Photos.requestCapture(client);
		}).dimensions(contentX(), buttonsY(1), contentWidth(), BUTTON_HEIGHT).build());

		int third = (contentWidth() - 2 * BUTTON_GAP) / 3;
		int y = buttonsY(0);
		ButtonWidget previous = addDrawableChild(ButtonWidget.builder(Text.literal("◀"), button -> show(lastIndex - 1))
			.dimensions(contentX(), y, third, BUTTON_HEIGHT).build());
		boolean armed = deleteArmedAt >= 0;
		ButtonWidget delete = addDrawableChild(ButtonWidget.builder(
			Text.translatable(armed ? "screen.pokebook.delete_confirm" : "screen.pokebook.delete"), button -> deleteCurrent())
			.dimensions(contentX() + third + BUTTON_GAP, y, third, BUTTON_HEIGHT).build());
		ButtonWidget next = addDrawableChild(ButtonWidget.builder(Text.literal("▶"), button -> show(lastIndex + 1))
			.dimensions(contentX() + 2 * (third + BUTTON_GAP), y, contentWidth() - 2 * (third + BUTTON_GAP), BUTTON_HEIGHT)
			.build());

		previous.active = lastIndex > 0;
		next.active = lastIndex < photos.size() - 1;
		delete.active = !photos.isEmpty();
	}

	private void show(int index) {
		if (index < 0 || index >= photos.size()) {
			return;
		}
		lastIndex = index;
		deleteArmedAt = -1;
		clearAndInit();
	}

	private void deleteCurrent() {
		if (photos.isEmpty()) {
			return;
		}
		// Dois cliques: a foto só existe neste computador, e apagar não tem volta.
		if (deleteArmedAt < 0 || System.currentTimeMillis() - deleteArmedAt >= CONFIRM_MS) {
			deleteArmedAt = System.currentTimeMillis();
			clearAndInit();
			return;
		}
		deleteArmedAt = -1;
		unload();
		Photos.delete(photos.get(lastIndex));
		clearAndInit();
	}

	@Override
	public void tick() {
		super.tick();
		if (deleteArmedAt >= 0 && System.currentTimeMillis() - deleteArmedAt >= CONFIRM_MS) {
			deleteArmedAt = -1;
			clearAndInit();
		}
	}

	// ------------------------------------------------------------------ textura

	/** Carrega a foto atual como textura, se ainda não for ela a carregada. */
	private boolean ensureLoaded() {
		if (photos.isEmpty() || client == null) {
			return false;
		}
		if (loadedIndex == lastIndex) {
			return true;
		}
		unload();
		try (InputStream input = Files.newInputStream(photos.get(lastIndex))) {
			NativeImage image = NativeImage.read(input);
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();
			// A textura passa a ser dona da imagem, e a fecha quando for destruída.
			client.getTextureManager().registerTexture(TEXTURE, new NativeImageBackedTexture(image));
			loadedIndex = lastIndex;
			return true;
		} catch (IOException e) {
			Pokebook.LOGGER.warn("Não deu para abrir a foto {}.", photos.get(lastIndex), e);
			return false;
		}
	}

	private void unload() {
		if (loadedIndex >= 0 && client != null) {
			client.getTextureManager().destroyTexture(TEXTURE);
		}
		loadedIndex = -1;
	}

	@Override
	public void removed() {
		// Sair da tela libera a memória de vídeo da foto. Vale também para ir ao menu.
		unload();
		super.removed();
	}

	// ------------------------------------------------------------------ desenho

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		int top = contentTop() + 2;
		int labelHeight = textRenderer.fontHeight + 3;
		int areaBottom = buttonsY(1) - BUTTON_GAP - labelHeight;
		int areaHeight = areaBottom - top;

		if (!ensureLoaded()) {
			Text empty = Text.translatable("screen.pokebook.photos.empty");
			context.drawText(textRenderer, empty,
				contentX() + (contentWidth() - textRenderer.getWidth(empty)) / 2, top + 16, COLOR_MUTED, false);
			return;
		}

		// Cabe inteira na área, sem deformar: a escala é a menor das duas.
		float scale = Math.min(contentWidth() / (float) imageWidth, areaHeight / (float) imageHeight);
		int width = Math.max(1, Math.round(imageWidth * scale));
		int height = Math.max(1, Math.round(imageHeight * scale));
		int x = contentX() + (contentWidth() - width) / 2;
		int y = top + (areaHeight - height) / 2;

		// ⚠️ A sobrecarga longa: o TAMANHO NA TELA vem antes de u,v, e depois o tamanho da
		// região e o da textura inteira. É a mesma que a moldura antiga usava — ver CLAUDE.md.
		context.drawTexture(TEXTURE, x, y, width, height, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight);

		Text counter = Text.translatable("screen.pokebook.photos.counter", lastIndex + 1, photos.size());
		context.drawText(textRenderer, counter,
			contentX() + (contentWidth() - textRenderer.getWidth(counter)) / 2, areaBottom + 2, COLOR_MUTED, false);
	}
}
