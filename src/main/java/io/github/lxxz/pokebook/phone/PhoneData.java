package io.github.lxxz.pokebook.phone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Tudo o que o aparelho guarda de um jogador: contatos, notas, pontos de interesse,
 * ajustes e a missão que ele está acompanhando.
 *
 * <p><b>Imutável de propósito.</b> A API de anexos do Fabric só marca o save como sujo
 * quando o anexo é <em>reatribuído</em> — mutar o objeto em lugar não basta, e já houve
 * bug por isso no progresso de missão. Com um registro imutável não há como esquecer: a
 * única forma de mudar algo é produzir um novo e atribuir.
 *
 * <p><b>Um pacote só leva tudo isto para o cliente</b>, com o mesmo codec que grava no
 * disco. É pouco dado — mesmo no limite, dezenas de KB —, e mandar inteiro a cada mudança
 * poupa um protocolo de diferenças que teria de ser escrito e depurado duas vezes.
 *
 * <p>Os limites existem porque o cliente não é confiável: sem eles, um cliente modificado
 * poderia encher o arquivo de save de um jogador até o servidor sofrer para carregá-lo.
 */
public record PhoneData(
	List<Contact> contacts,
	List<String> notes,
	List<Waypoint> waypoints,
	PhoneSettings settings,
	Optional<Identifier> trackedMission
) {
	public static final int MAX_CONTACTS = 100;
	public static final int MAX_NOTES = 50;
	public static final int MAX_NOTE_LENGTH = 1000;
	public static final int MAX_WAYPOINTS = 50;

	public static final PhoneData EMPTY =
		new PhoneData(List.of(), List.of(), List.of(), PhoneSettings.DEFAULT, Optional.empty());

	/** Todos os campos opcionais: um save de antes de cada funcionalidade existir carrega sem erro. */
	public static final Codec<PhoneData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Contact.CODEC.listOf().optionalFieldOf("contacts", List.of()).forGetter(PhoneData::contacts),
		Codec.STRING.listOf().optionalFieldOf("notes", List.of()).forGetter(PhoneData::notes),
		Waypoint.CODEC.listOf().optionalFieldOf("waypoints", List.of()).forGetter(PhoneData::waypoints),
		PhoneSettings.CODEC.optionalFieldOf("settings", PhoneSettings.DEFAULT).forGetter(PhoneData::settings),
		Identifier.CODEC.optionalFieldOf("tracked_mission").forGetter(PhoneData::trackedMission)
	).apply(instance, PhoneData::new));

	public PhoneData {
		// Cópias imutáveis: o codec entrega listas que podem ser mutáveis por baixo, e um
		// registro "imutável" com lista mutável dentro não é imutável.
		contacts = List.copyOf(contacts);
		notes = List.copyOf(notes);
		waypoints = List.copyOf(waypoints);
	}

	// ------------------------------------------------------------------ contatos

	public Optional<Contact> contact(UUID uuid) {
		return contacts.stream().filter(c -> c.uuid().equals(uuid)).findFirst();
	}

	public boolean hasContact(UUID uuid) {
		return contact(uuid).isPresent();
	}

	public PhoneData withContacts(List<Contact> newContacts) {
		return new PhoneData(newContacts, notes, waypoints, settings, trackedMission);
	}

	/** Troca um contato pelo resultado da função, se ele existir. */
	public PhoneData updateContact(UUID uuid, UnaryOperator<Contact> change) {
		List<Contact> updated = new ArrayList<>(contacts.size());
		for (Contact contact : contacts) {
			updated.add(contact.uuid().equals(uuid) ? change.apply(contact) : contact);
		}
		return withContacts(updated);
	}

	// ------------------------------------------------------------------ o resto

	public PhoneData withNotes(List<String> newNotes) {
		return new PhoneData(contacts, newNotes, waypoints, settings, trackedMission);
	}

	public PhoneData withWaypoints(List<Waypoint> newWaypoints) {
		return new PhoneData(contacts, notes, newWaypoints, settings, trackedMission);
	}

	public PhoneData withSettings(PhoneSettings newSettings) {
		return new PhoneData(contacts, notes, waypoints, newSettings, trackedMission);
	}

	public PhoneData withTrackedMission(Optional<Identifier> mission) {
		return new PhoneData(contacts, notes, waypoints, settings, mission);
	}
}
