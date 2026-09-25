package io.github.lxxz.pokebook.message;

import com.mojang.serialization.Codec;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.config.ServerConfig;
import io.github.lxxz.pokebook.network.ConversationSummary;
import io.github.lxxz.pokebook.network.ConversationsPayload;
import io.github.lxxz.pokebook.network.MessageArrivedPayload;
import io.github.lxxz.pokebook.network.MessageLimits;
import io.github.lxxz.pokebook.network.ThreadMessage;
import io.github.lxxz.pokebook.network.ThreadPayload;
import io.github.lxxz.pokebook.notify.NotificationKind;
import io.github.lxxz.pokebook.notify.Notifications;
import io.github.lxxz.pokebook.phone.Contact;
import io.github.lxxz.pokebook.phone.PhoneService;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Mensagens entre jogadores, com entrega para quem está offline.
 *
 * <p><b>A caixa postal mora no mundo</b>, num anexo do mundo principal — o mesmo mecanismo do
 * ranking. É o que faz a mensagem esperar o destinatário: no save dele não dá para escrever
 * com ele fora.
 *
 * <p><b>Consentimento:</b> só chega a quem salvou quem manda. É a regra de contatos que vale
 * para tudo — consultada no {@link ContactDirectory}, que funciona com o destinatário offline.
 * O "não perturbe" não bloqueia mensagem: ela fica guardada, e só o aviso é silenciado.
 *
 * <p><b>Limites:</b> {@link MessageLimits#MAX_TEXT} caracteres, uma por segundo por pessoa,
 * {@link Conversation#MAX_MESSAGES} por conversa. <b>Registro:</b> toda mensagem vai para o
 * log do servidor, com remetente e destinatário. Num servidor público o dono vai precisar
 * disso para moderar, e quem manda deve saber que é assim — está no {@code README}.
 */
public final class MessageService {
	private record Mailbox(Map<String, Conversation> conversations) {
		static final Codec<Mailbox> CODEC = Codec.unboundedMap(Codec.STRING, Conversation.CODEC)
			.xmap(Mailbox::new, Mailbox::conversations);

		Mailbox {
			conversations = Map.copyOf(conversations);
		}
	}

	private static final AttachmentType<Mailbox> MAILBOX = AttachmentRegistry.create(
		Identifier.of(Pokebook.MOD_ID, "mailbox"),
		builder -> builder
			.initializer(() -> new Mailbox(Map.of()))
			.persistent(Mailbox.CODEC)
	);

	/** Espera mínima entre duas mensagens do mesmo jogador, em ticks. */
	private static final int COOLDOWN_TICKS = 20;

	private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

	private MessageService() {
	}

	public static void register() {
	}

	private static Mailbox mailbox(MinecraftServer server) {
		return server.getOverworld().getAttachedOrCreate(MAILBOX);
	}

	private static void save(MinecraftServer server, String key, Conversation conversation) {
		ServerWorld overworld = server.getOverworld();
		Map<String, Conversation> updated = new HashMap<>(overworld.getAttachedOrCreate(MAILBOX).conversations());
		updated.put(key, conversation);
		overworld.setAttached(MAILBOX, new Mailbox(updated));
	}

	/** Pode mandar? O destinatário precisa ter salvo o remetente. */
	public static boolean canMessage(ServerPlayerEntity sender, UUID recipient) {
		return !sender.getUuid().equals(recipient)
			&& ContactDirectory.hasSaved(sender.server, recipient, sender.getUuid());
	}

	/**
	 * Manda uma mensagem — de texto, ou de foto se {@code photo} vier preenchido.
	 *
	 * @return se foi entregue à caixa postal; se não, o motivo já foi dito a quem mandou
	 */
	public static boolean send(ServerPlayerEntity sender, UUID recipient, String text, Optional<String> photo) {
		MinecraftServer server = sender.server;
		if (!ServerConfig.features().messages()) {
			sender.sendMessage(Text.translatable("message.pokebook.feature_disabled"), true);
			return false;
		}
		if (!canMessage(sender, recipient)) {
			sender.sendMessage(Text.translatable("message.pokebook.messages.not_accepting"), true);
			return false;
		}

		String clean = StringHelper.stripInvalidChars(text).strip();
		if (clean.length() > MessageLimits.MAX_TEXT) {
			clean = clean.substring(0, MessageLimits.MAX_TEXT);
		}
		if (clean.isEmpty() && photo.isEmpty()) {
			return false;
		}

		int now = server.getTicks();
		Integer last = LAST_SENT.get(sender.getUuid());
		if (last != null && now - last < COOLDOWN_TICKS) {
			sender.sendMessage(Text.translatable("message.pokebook.messages.cooldown"), true);
			return false;
		}
		LAST_SENT.put(sender.getUuid(), now);

		String senderName = sender.getGameProfile().getName();
		ServerPlayerEntity online = server.getPlayerManager().getPlayer(recipient);
		String key = Conversation.key(sender.getUuid(), recipient);
		Conversation conversation = mailbox(server).conversations().get(key);
		if (conversation == null) {
			conversation = Conversation.start(sender.getUuid(), senderName, recipient, nameFor(server, sender, recipient));
		}
		long time = System.currentTimeMillis();
		conversation = conversation.with(new Conversation.Message(sender.getUuid(), clean, time, photo), senderName)
			// Quem manda já leu tudo até aqui: a própria mensagem não conta como nova para ele.
			.readNowBy(sender.getUuid(), time);
		save(server, key, conversation);

		Pokebook.LOGGER.info("[Pokébook] mensagem {} -> {}: {}{}", senderName, conversation.nameOf(recipient),
			clean, photo.map(id -> " [foto " + id + "]").orElse(""));

		ServerPlayNetworking.send(sender, new MessageArrivedPayload(recipient));
		if (online != null) {
			ServerPlayNetworking.send(online, new MessageArrivedPayload(sender.getUuid()));
			Notifications.send(online, NotificationKind.MESSAGE,
				Text.translatable("notification.pokebook.message.title", senderName),
				photo.isPresent() ? Text.translatable("notification.pokebook.message.photo") : Text.literal(clean));
		}
		return true;
	}

	/** O nome do destinatário: o de agora, se online; senão o que o remetente tem salvo. */
	private static String nameFor(MinecraftServer server, ServerPlayerEntity sender, UUID recipient) {
		ServerPlayerEntity online = server.getPlayerManager().getPlayer(recipient);
		if (online != null) {
			return online.getGameProfile().getName();
		}
		return PhoneService.get(sender).contact(recipient)
			.map(Contact::name)
			.orElse("?");
	}

	public static void sendConversations(ServerPlayerEntity player) {
		UUID self = player.getUuid();
		List<ConversationSummary> summaries = new ArrayList<>();
		for (Conversation conversation : mailbox(player.server).conversations().values()) {
			if (!conversation.involves(self) || conversation.messages().isEmpty()) {
				continue;
			}
			Conversation.Message last = conversation.messages().get(conversation.messages().size() - 1);
			UUID other = conversation.other(self);
			String preview = last.photo().isPresent() && last.text().isEmpty()
				? Text.translatable("notification.pokebook.message.photo").getString()
				: last.text();
			summaries.add(new ConversationSummary(other, conversation.nameOf(other), preview, last.time(),
				conversation.unreadFor(self)));
		}
		summaries.sort(Comparator.comparingLong(ConversationSummary::time).reversed());
		ServerPlayNetworking.send(player, new ConversationsPayload(summaries));
	}

	/** Manda a conversa e a marca como lida — pedir a conversa é abri-la. */
	public static void sendThread(ServerPlayerEntity player, UUID other) {
		UUID self = player.getUuid();
		MinecraftServer server = player.server;
		String key = Conversation.key(self, other);
		Conversation conversation = mailbox(server).conversations().get(key);

		List<ThreadMessage> messages = new ArrayList<>();
		String name;
		if (conversation != null) {
			for (Conversation.Message message : conversation.messages()) {
				messages.add(new ThreadMessage(message.from().equals(self), message.text(), message.time(), message.photo()));
			}
			name = conversation.nameOf(other);
			if (conversation.unreadFor(self) > 0) {
				save(server, key, conversation.readNowBy(self, System.currentTimeMillis()));
			}
		} else {
			name = PhoneService.get(player).contact(other)
				.map(Contact::name).orElse("?");
		}
		ServerPlayNetworking.send(player, new ThreadPayload(other, name, messages));
	}

	/** Esta pessoa participa de uma conversa em que esta foto foi mandada? Autoriza vê-la. */
	public static boolean canSeePhoto(ServerPlayerEntity player, String photoId) {
		for (Conversation conversation : mailbox(player.server).conversations().values()) {
			if (conversation.involves(player.getUuid()) && conversation.containsPhoto(photoId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Ao entrar: se chegou mensagem enquanto estava fora, avisa uma vez, com o total.
	 *
	 * <p>Um aviso só, e não um por mensagem: voltar de uma semana fora não pode ser uma
	 * cascata de notificações.
	 */
	public static void onJoin(ServerPlayerEntity player) {
		int unread = 0;
		for (Conversation conversation : mailbox(player.server).conversations().values()) {
			if (conversation.involves(player.getUuid())) {
				unread += conversation.unreadFor(player.getUuid());
			}
		}
		if (unread > 0) {
			Notifications.send(player, NotificationKind.MESSAGE,
				Text.translatable("notification.pokebook.message.waiting.title"),
				Text.translatable("notification.pokebook.message.waiting.body", unread));
		}
	}

	public static void disconnect(ServerPlayerEntity player) {
		LAST_SENT.remove(player.getUuid());
	}
}
