package io.github.lxxz.pokebook.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * O rosto de um jogador, conectado ou não.
 *
 * <p>Quem está conectado tem a skin de verdade na lista de jogadores do cliente — a mesma da
 * tecla Tab. Quem não está cai na skin padrão que o jogo daria àquela conta: não é o rosto
 * dele, mas é sempre o mesmo para a mesma pessoa, o que já ajuda a reconhecer.
 *
 * <p>O véu escuro sobre quem está offline diz "não está aqui agora" sem precisar mexer nos
 * pixels da skin.
 */
public final class PlayerFaces {
	private static final int OFFLINE_VEIL = 0xA0202020;

	private PlayerFaces() {
	}

	/** Por {@link UUID}, que é a identidade — usado quando se sabe quem é, como num contato. */
	public static Identifier skin(UUID uuid) {
		ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
		if (handler != null) {
			PlayerListEntry listed = handler.getPlayerListEntry(uuid);
			if (listed != null) {
				return listed.getSkinTextures().texture();
			}
		}
		return DefaultSkinHelper.getSkinTextures(uuid).texture();
	}

	/** Por nome, quando é tudo o que se tem — o ranking só guarda o nome. */
	public static Identifier skin(String name) {
		ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
		if (handler != null) {
			PlayerListEntry listed = handler.getPlayerListEntry(name);
			if (listed != null) {
				return listed.getSkinTextures().texture();
			}
		}
		return DefaultSkinHelper.getSkinTextures(Uuids.getOfflinePlayerUuid(name)).texture();
	}

	public static void draw(DrawContext context, Identifier skin, int x, int y, int size, boolean online) {
		PlayerSkinDrawer.draw(context, skin, x, y, size);
		if (!online) {
			context.fill(x, y, x + size, y + size, OFFLINE_VEIL);
		}
	}

	/** Está na lista de jogadores do cliente agora? */
	public static boolean isOnline(UUID uuid) {
		ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
		return handler != null && handler.getPlayerListEntry(uuid) != null;
	}
}
