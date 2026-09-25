package io.github.lxxz.pokebook.phone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Alguém que um jogador salvou nos contatos.
 *
 * <p><b>O {@link UUID} é a identidade; o nome é só para mostrar.</b> Guardar o nome junto
 * é o que faz o contato aparecer na lista mesmo com a pessoa offline — o servidor só sabe
 * o nome de quem está conectado. Ele é atualizado quando a pessoa entra, para acompanhar
 * troca de apelido.
 *
 * <p>{@code shareLocation} é uma decisão <b>de quem salvou</b>: "eu deixo este contato ver
 * onde eu estou". É o dono da lista que abre mão da própria privacidade, nunca o outro —
 * por isso mora aqui e não no contato do lado de lá.
 */
public record Contact(UUID uuid, String name, boolean shareLocation) {
	public static final Codec<Contact> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Uuids.CODEC.fieldOf("uuid").forGetter(Contact::uuid),
		Codec.STRING.fieldOf("name").forGetter(Contact::name),
		Codec.BOOL.optionalFieldOf("share_location", false).forGetter(Contact::shareLocation)
	).apply(instance, Contact::new));

	public Contact withName(String newName) {
		return new Contact(uuid, newName, shareLocation);
	}

	public Contact withShareLocation(boolean share) {
		return new Contact(uuid, name, share);
	}
}
