package io.github.lxxz.pokebook.message;

import com.mojang.serialization.Codec;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.phone.Contact;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Quem salvou quem, guardado no mundo — para decidir consentimento com a pessoa offline.
 *
 * <p><b>Por que existe.</b> Mensagem só chega a quem salvou quem manda: o consentimento é de
 * quem recebe. Mas a caixa postal existe justamente para quem está offline, e os contatos de
 * um jogador offline estão no save dele, fora de alcance. Este é um espelho, no mundo
 * principal, só dos UUIDs — atualizado toda vez que os contatos de alguém mudam.
 *
 * <p>O dado de verdade continua no {@code PhoneData}. Se os dois divergirem (um save de antes
 * deste espelho existir), o espelho se corrige quando o dono entra — ver
 * {@code PhoneService.onJoin}. Até lá, quem nunca entrou depois da atualização aparece como
 * sem contatos, e recebe só quando estiver online: o lado seguro do erro.
 */
public final class ContactDirectory {
	private record Directory(Map<UUID, Set<UUID>> contacts) {
		static final Codec<Directory> CODEC = Codec.unboundedMap(Uuids.STRING_CODEC, Uuids.SET_CODEC)
			.xmap(Directory::new, Directory::contacts);

		Directory {
			contacts = Map.copyOf(contacts);
		}
	}

	private static final AttachmentType<Directory> DIRECTORY = AttachmentRegistry.create(
		Identifier.of(Pokebook.MOD_ID, "contact_directory"),
		builder -> builder
			.initializer(() -> new Directory(Map.of()))
			.persistent(Directory.CODEC)
	);

	private ContactDirectory() {
	}

	public static void register() {
	}

	/** Grava os contatos atuais de alguém, se mudaram. */
	public static void update(MinecraftServer server, UUID owner, List<Contact> contacts) {
		ServerWorld overworld = server.getOverworld();
		Directory directory = overworld.getAttachedOrCreate(DIRECTORY);
		Set<UUID> now = new HashSet<>();
		for (Contact contact : contacts) {
			now.add(contact.uuid());
		}
		if (now.equals(directory.contacts().get(owner))) {
			return;
		}
		Map<UUID, Set<UUID>> updated = new HashMap<>(directory.contacts());
		updated.put(owner, Set.copyOf(now));
		overworld.setAttached(DIRECTORY, new Directory(updated));
	}

	/** O dono desta lista salvou aquela pessoa? */
	public static boolean hasSaved(MinecraftServer server, UUID owner, UUID other) {
		Set<UUID> saved = server.getOverworld().getAttachedOrCreate(DIRECTORY).contacts().get(owner);
		return saved != null && saved.contains(other);
	}
}
