package io.github.lxxz.pokebook.registry;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.block.PokebookBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
	public static final Block POKEBOOK = new PokebookBlock(
		AbstractBlock.Settings.create()
			.strength(1.5f)
			.sounds(BlockSoundGroup.METAL)
			// O modelo não preenche o cubo: sem isto, as faces dos blocos vizinhos somem.
			.nonOpaque()
			.luminance(state -> PokebookBlock.lightFor(state.get(PokebookBlock.SCREEN)))
	);

	private ModBlocks() {
	}

	public static void register() {
		Identifier id = Identifier.of(Pokebook.MOD_ID, "pokebook");
		Registry.register(Registries.BLOCK, id, POKEBOOK);
		Registry.register(Registries.ITEM, id, new BlockItem(POKEBOOK, new Item.Settings()));
	}
}
