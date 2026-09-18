package io.github.lxxz.pokebook.registry;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.item.PokephoneItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
	/** Empilhar não faria sentido: é um aparelho, e um basta. */
	public static final Item POKEPHONE = new PokephoneItem(new Item.Settings().maxCount(1));

	private ModItems() {
	}

	public static void register() {
		Registry.register(Registries.ITEM, Identifier.of(Pokebook.MOD_ID, "pokephone"), POKEPHONE);
	}
}
