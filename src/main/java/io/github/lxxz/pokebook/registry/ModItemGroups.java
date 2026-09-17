package io.github.lxxz.pokebook.registry;

import io.github.lxxz.pokebook.Pokebook;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
	public static final ItemGroup MAIN = FabricItemGroup.builder()
		.icon(() -> new ItemStack(ModBlocks.POKEBOOK))
		.displayName(Text.translatable("itemGroup.pokebook.main"))
		.entries((context, entries) -> entries.add(ModBlocks.POKEBOOK))
		.build();

	private ModItemGroups() {
	}

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, Identifier.of(Pokebook.MOD_ID, "main"), MAIN);
	}
}
