package io.github.lxxz.pokebook.phone;

import io.github.lxxz.pokebook.config.ServerConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manda uma localização no chat, como texto clicável que liga a seta de quem clicar.
 *
 * <p>O vanilla não tem ação de clique melhor que <b>rodar um comando</b>, então o clique
 * roda {@code /pokebook rastrear}, que é nosso. Com isso qualquer jogador com o mod vê a
 * seta; quem não tem o mod vê as coordenadas escritas, que continuam servindo.
 *
 * <p>Para todo mundo no chat, e não só para contatos: é o equivalente a digitar as
 * coordenadas, que qualquer um já pode fazer. Compartilhar com alguém específico, e de
 * forma contínua, é o {@link LocationSharing}.
 */
public final class ChatLocation {
	/**
	 * Espera mínima entre dois envios do mesmo jogador, em ticks.
	 *
	 * <p>É um botão numa tela; sem isto, segurá-lo ou um cliente modificado inundariam o
	 * chat de todo mundo.
	 */
	private static final int COOLDOWN_TICKS = 5 * 20;

	private static final Map<UUID, Integer> LAST_SHARE = new HashMap<>();

	private ChatLocation() {
	}

	public static void share(ServerPlayerEntity player, Optional<Waypoint> waypoint) {
		if (!ServerConfig.features().chatLocation()) {
			player.sendMessage(Text.translatable("message.pokebook.feature_disabled"), true);
			return;
		}

		int now = player.server.getTicks();
		Integer last = LAST_SHARE.get(player.getUuid());
		if (last != null && now - last < COOLDOWN_TICKS) {
			player.sendMessage(Text.translatable("message.pokebook.location.cooldown"), true);
			return;
		}
		LAST_SHARE.put(player.getUuid(), now);

		String who = player.getGameProfile().getName();
		GlobalPos pos;
		String label;
		if (waypoint.isPresent()) {
			Waypoint clean = waypoint.get().sanitized();
			pos = clean.pos();
			label = clean.name();
		} else {
			// "Onde eu estou" usa a posição que o servidor sabe, não uma que o cliente diria.
			pos = GlobalPos.create(player.getServerWorld().getRegistryKey(), player.getBlockPos());
			label = who;
		}

		player.server.getPlayerManager().broadcast(message(who, label, pos), false);
	}

	private static Text message(String who, String label, GlobalPos pos) {
		BlockPos p = pos.pos();
		String command = "/pokebook rastrear %d %d %d %s %s".formatted(
			p.getX(), p.getY(), p.getZ(), pos.dimension().getValue(), label);

		MutableText coordinates = Texts.bracketed(Text.literal("%d, %d, %d".formatted(p.getX(), p.getY(), p.getZ())))
			.styled(style -> style
				.withColor(Formatting.GREEN)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					Text.translatable("message.pokebook.location.click_to_track"))));

		return Text.translatable("message.pokebook.location.shared", who, label, coordinates);
	}

	public static void disconnect(ServerPlayerEntity player) {
		LAST_SHARE.remove(player.getUuid());
	}
}
