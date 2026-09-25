package io.github.lxxz.pokebook.battle;

import io.github.lxxz.pokebook.notify.NotificationKind;
import io.github.lxxz.pokebook.notify.Notifications;
import io.github.lxxz.pokebook.phone.PhoneService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Desafio de batalha pelo celular: convidar alguém, esteja onde estiver.
 *
 * <p><b>Por que não o desafio do próprio Cobblemon.</b> O dele exige estar perto — ele confere
 * a distância ao enviar, porque o gesto é clicar no outro jogador. Pelo celular o sentido é o
 * contrário: desafiar quem está longe. Então o convite é nosso, e só o <em>começo da
 * batalha</em> passa pelo Cobblemon, no aceite.
 *
 * <p><b>Esta classe não menciona o Cobblemon.</b> Quem começa a batalha é um
 * {@link Starter} que a integração instala ao carregar — sem o Cobblemon ninguém instala, e
 * {@link #available()} responde que não. É a mesma fronteira das missões e das ligações.
 *
 * <p>O aceite é por <b>texto clicável no chat</b>, como os pedidos do próprio Cobblemon e do
 * vanilla. Não pede tela nova: quem é desafiado talvez nem esteja com o celular na mão.
 *
 * <p>O consentimento é o mesmo da ligação: quem aceita ligação de você aceita desafio de
 * você, e o "não perturbe" bloqueia os dois.
 */
public final class BattleChallenges {
	/** Quanto um desafio espera resposta. Um minuto: dá tempo de ler e decidir. */
	private static final int EXPIRY_TICKS = 60 * 20;

	/** Quem começa a batalha. Instalado pela integração com o Cobblemon. */
	@FunctionalInterface
	public interface Starter {
		/** @return se a batalha começou; se não, o motivo já foi dito aos dois */
		boolean start(ServerPlayerEntity challenger, ServerPlayerEntity challenged);
	}

	private record Pending(UUID challenger, int createdAt) {
	}

	private static Starter starter;

	/** Desafios esperando resposta, por quem foi desafiado. Um por pessoa de cada vez. */
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private BattleChallenges() {
	}

	public static void setStarter(Starter newStarter) {
		starter = newStarter;
	}

	public static boolean available() {
		return starter != null;
	}

	public static void challenge(ServerPlayerEntity challenger, UUID targetId) {
		if (!available()) {
			challenger.sendMessage(Text.translatable("message.pokebook.feature_disabled"), true);
			return;
		}
		ServerPlayerEntity target = challenger.server.getPlayerManager().getPlayer(targetId);
		if (target == null || target == challenger) {
			return;
		}
		String targetName = target.getGameProfile().getName();
		if (!PhoneService.acceptsCallFrom(target, challenger)) {
			challenger.sendMessage(Text.translatable("message.pokebook.battle.not_accepting", targetName), false);
			return;
		}
		if (PENDING.containsKey(targetId)) {
			challenger.sendMessage(Text.translatable("message.pokebook.battle.busy", targetName), false);
			return;
		}

		PENDING.put(targetId, new Pending(challenger.getUuid(), challenger.server.getTicks()));
		String challengerName = challenger.getGameProfile().getName();

		challenger.sendMessage(Text.translatable("message.pokebook.battle.sent", targetName), false);
		target.sendMessage(Text.translatable("message.pokebook.battle.received", challengerName,
			button("message.pokebook.battle.accept", Formatting.GREEN, "/pokebook batalha aceitar " + challengerName),
			button("message.pokebook.battle.decline", Formatting.RED, "/pokebook batalha recusar " + challengerName)), false);
		Notifications.send(target, NotificationKind.CALL,
			Text.translatable("notification.pokebook.battle.title"),
			Text.translatable("notification.pokebook.battle.body", challengerName));
	}

	private static MutableText button(String key, Formatting color, String command) {
		return Texts.bracketed(Text.translatable(key)).styled(style -> style
			.withColor(color)
			.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command))));
	}

	/** Aceita o desafio que este jogador recebeu daquele. Vem do comando que o clique roda. */
	public static void accept(ServerPlayerEntity challenged, String challengerName) {
		ServerPlayerEntity challenger = take(challenged, challengerName);
		if (challenger == null) {
			challenged.sendMessage(Text.translatable("message.pokebook.battle.none", challengerName), false);
			return;
		}
		if (!starter.start(challenger, challenged)) {
			Text failure = Text.translatable("message.pokebook.battle.failed");
			challenger.sendMessage(failure, false);
			challenged.sendMessage(failure, false);
		}
	}

	public static void decline(ServerPlayerEntity challenged, String challengerName) {
		ServerPlayerEntity challenger = take(challenged, challengerName);
		if (challenger != null) {
			challenger.sendMessage(Text.translatable("message.pokebook.battle.declined",
				challenged.getGameProfile().getName()), false);
		}
	}

	/**
	 * Tira o desafio da espera, se ele existe e é mesmo daquele jogador.
	 *
	 * <p>O nome vem de um comando que qualquer um pode digitar; ele só serve para achar o
	 * desafio — quem autoriza é o registro de espera, que só o servidor escreve.
	 */
	private static ServerPlayerEntity take(ServerPlayerEntity challenged, String challengerName) {
		Pending pending = PENDING.get(challenged.getUuid());
		if (pending == null) {
			return null;
		}
		ServerPlayerEntity challenger = challenged.server.getPlayerManager().getPlayer(pending.challenger());
		if (challenger == null || !challenger.getGameProfile().getName().equalsIgnoreCase(challengerName)) {
			return null;
		}
		PENDING.remove(challenged.getUuid());
		return challenger;
	}

	/** Desafio sem resposta por um minuto expira, e quem desafiou fica sabendo. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty() || server.getTicks() % 20 != 0) {
			return;
		}
		List<UUID> expired = new ArrayList<>();
		PENDING.forEach((target, pending) -> {
			if (server.getTicks() - pending.createdAt() >= EXPIRY_TICKS) {
				expired.add(target);
			}
		});
		for (UUID target : expired) {
			Pending pending = PENDING.remove(target);
			ServerPlayerEntity challenger = server.getPlayerManager().getPlayer(pending.challenger());
			if (challenger != null) {
				challenger.sendMessage(Text.translatable("message.pokebook.battle.expired"), false);
			}
		}
	}

	/** Quem sai leva junto os desafios em que está, dos dois lados. */
	public static void disconnect(ServerPlayerEntity player) {
		PENDING.remove(player.getUuid());
		PENDING.values().removeIf(pending -> pending.challenger().equals(player.getUuid()));
	}
}
