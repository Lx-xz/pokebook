package io.github.lxxz.pokebook.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Uma conversa entre duas pessoas: as mensagens, os nomes e até onde cada um já leu.
 *
 * <p>Imutável, pelo mesmo motivo de {@code PhoneData}: mora num anexo, e anexo só é marcado
 * como sujo ao ser reatribuído.
 *
 * <p>Os nomes ficam guardados aqui porque a conversa existe com os dois offline — é a razão
 * de ser da caixa postal —, e o servidor só sabe o nome de quem está conectado.
 *
 * <p>"Lido" é um instante, não uma marca por mensagem: tudo o que chegou depois de
 * {@code readBy[pessoa]} e não foi escrito por ela conta como novo. Um número por pessoa em
 * vez de um por mensagem.
 */
public record Conversation(UUID first, UUID second, Map<UUID, String> names, List<Message> messages,
		Map<UUID, Long> readBy) {
	/** Quantas mensagens uma conversa guarda. As mais velhas saem para as novas entrarem. */
	public static final int MAX_MESSAGES = 100;

	/** Uma mensagem. {@code photo} é o id de uma foto compartilhada, se for mensagem de foto. */
	public record Message(UUID from, String text, long time, Optional<String> photo) {
		public static final Codec<Message> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Uuids.CODEC.fieldOf("from").forGetter(Message::from),
			Codec.STRING.fieldOf("text").forGetter(Message::text),
			Codec.LONG.fieldOf("time").forGetter(Message::time),
			Codec.STRING.optionalFieldOf("photo").forGetter(Message::photo)
		).apply(instance, Message::new));
	}

	public static final Codec<Conversation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Uuids.CODEC.fieldOf("first").forGetter(Conversation::first),
		Uuids.CODEC.fieldOf("second").forGetter(Conversation::second),
		// Mapa em NBT exige chave de texto; o codec padrão de UUID é um vetor de inteiros.
		Codec.unboundedMap(Uuids.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(Conversation::names),
		Message.CODEC.listOf().optionalFieldOf("messages", List.of()).forGetter(Conversation::messages),
		Codec.unboundedMap(Uuids.STRING_CODEC, Codec.LONG).optionalFieldOf("read_by", Map.of()).forGetter(Conversation::readBy)
	).apply(instance, Conversation::new));

	public Conversation {
		names = Map.copyOf(names);
		messages = List.copyOf(messages);
		readBy = Map.copyOf(readBy);
	}

	/** A chave de uma conversa: os dois UUIDs em ordem, para A→B e B→A caírem no mesmo lugar. */
	public static String key(UUID a, UUID b) {
		return a.compareTo(b) <= 0 ? a + "_" + b : b + "_" + a;
	}

	public static Conversation start(UUID a, String nameA, UUID b, String nameB) {
		return new Conversation(a, b, Map.of(a, nameA, b, nameB), List.of(), Map.of());
	}

	public boolean involves(UUID player) {
		return first.equals(player) || second.equals(player);
	}

	public UUID other(UUID player) {
		return first.equals(player) ? second : first;
	}

	public String nameOf(UUID player) {
		return names.getOrDefault(player, "?");
	}

	/** Acrescenta uma mensagem, descartando a mais velha além do limite, e atualiza o nome de quem mandou. */
	public Conversation with(Message message, String senderName) {
		List<Message> updated = new ArrayList<>(messages);
		updated.add(message);
		while (updated.size() > MAX_MESSAGES) {
			updated.remove(0);
		}
		Map<UUID, String> updatedNames = new HashMap<>(names);
		updatedNames.put(message.from(), senderName);
		return new Conversation(first, second, updatedNames, updated, readBy);
	}

	public Conversation readNowBy(UUID player, long time) {
		Map<UUID, Long> updated = new HashMap<>(readBy);
		updated.put(player, time);
		return new Conversation(first, second, names, messages, updated);
	}

	public int unreadFor(UUID player) {
		long read = readBy.getOrDefault(player, 0L);
		int count = 0;
		for (Message message : messages) {
			if (!message.from().equals(player) && message.time() > read) {
				count++;
			}
		}
		return count;
	}

	public boolean containsPhoto(String photoId) {
		return messages.stream().anyMatch(m -> m.photo().filter(photoId::equals).isPresent());
	}
}
