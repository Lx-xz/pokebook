package io.github.lxxz.pokebook.phone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.GlobalPos;

import java.util.List;

/**
 * Um ponto de interesse: nome, lugar e ícone.
 *
 * <p>{@link GlobalPos} e não só {@code BlockPos} porque as mesmas coordenadas existem em
 * cada dimensão — um ponto no Nether sem a dimensão junto levaria a seta para o lugar
 * errado do mundo principal. É o mesmo cuidado de {@code PokebookViewers.Key}.
 *
 * <p><b>O ícone é um item do jogo</b>, desenhado com o próprio desenho do item. Não há arte
 * nova a fazer e o jogador reconhece de relance — uma cama é casa, um baú é depósito. A
 * escolha fica restrita a {@link #ICONS} por dois motivos: o servidor não aceita qualquer
 * identificador vindo do cliente, e uma lista curta cabe numa fileira de botões.
 */
public record Waypoint(String name, GlobalPos pos, Identifier icon) {
	/** Comprimento máximo do nome. Cabe numa linha da lista do poképhone. */
	public static final int MAX_NAME_LENGTH = 24;

	/** Os ícones oferecidos, na ordem em que aparecem para escolher. O primeiro é o padrão. */
	public static final List<Item> ICONS = List.of(
		Items.COMPASS, Items.RED_BED, Items.CHEST, Items.CRAFTING_TABLE, Items.IRON_PICKAXE,
		Items.WHEAT, Items.OAK_SAPLING, Items.ENDER_EYE, Items.DIAMOND, Items.SKELETON_SKULL
	);

	public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("name").forGetter(Waypoint::name),
		GlobalPos.CODEC.fieldOf("pos").forGetter(Waypoint::pos),
		Identifier.CODEC.optionalFieldOf("icon", defaultIcon()).forGetter(Waypoint::icon)
	).apply(instance, Waypoint::new));

	public static Identifier defaultIcon() {
		return Registries.ITEM.getId(ICONS.get(0));
	}

	/** O ícone como item, caindo no padrão se o salvo não estiver mais na lista. */
	public Item iconItem() {
		for (Item item : ICONS) {
			if (Registries.ITEM.getId(item).equals(icon)) {
				return item;
			}
		}
		return ICONS.get(0);
	}

	/**
	 * A mesma coisa, com nome e ícone passados pelo crivo do servidor.
	 *
	 * <p>O cliente pode mandar qualquer texto e qualquer identificador. Aqui o nome é
	 * aparado e cortado no limite, vazio vira "?", e ícone fora da lista vira o padrão.
	 *
	 * <p>⚠️ <b>Caracteres inválidos saem antes de tudo, e não é capricho.</b> O nome vai
	 * parar dentro do comando que o texto clicável executa quando a localização é mandada no
	 * chat. Um {@code §} ali faria o servidor <b>expulsar quem clicou</b> por "caractere
	 * ilegal no chat" — um cliente modificado transformaria um ponto de interesse numa
	 * armadilha para os outros.
	 */
	public Waypoint sanitized() {
		String clean = StringHelper.stripInvalidChars(name).strip();
		if (clean.length() > MAX_NAME_LENGTH) {
			clean = clean.substring(0, MAX_NAME_LENGTH);
		}
		if (clean.isEmpty()) {
			clean = "?";
		}
		return new Waypoint(clean, pos, Registries.ITEM.getId(iconItem()));
	}
}
