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

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
	public static final Block POKEBOOK = new PokebookBlock(settings());

	/**
	 * Cores de protótipo, com o valor RGB do corante correspondente do vanilla.
	 *
	 * <p><b>Não existe arte por cor.</b> Todas usam o mesmo atlas e os mesmos dois modelos;
	 * a cor entra por tingimento, que multiplica a textura pela cor pedida. O chassi foi
	 * desenhado claro (74% dos pixels acima de 204 de luminância), e é isso que permite
	 * cores vivas — multiplicação só escurece.
	 *
	 * <p>As faces da tela e do teclado <b>não</b> levam {@code tintindex} no modelo, senão
	 * um pokébook vermelho ganharia tela vermelha.
	 *
	 * <p>Blocos separados por cor, e não uma propriedade de blockstate, pelo mesmo motivo
	 * que a lã e o concreto do vanilla são blocos separados: o item carrega a cor de graça
	 * por ser outro item, em vez de exigir que o {@code BlockItem} preserve blockstate.
	 */
	public static final Map<String, Integer> TINTS = new LinkedHashMap<>();

	/** O bloco de cada cor. A mesma classe e as mesmas propriedades do pokébook branco. */
	public static final Map<String, Block> TINTED = new LinkedHashMap<>();

	static {
		TINTS.put("red", 0xB02E26);
		TINTS.put("blue", 0x3C44AA);
		TINTS.put("lime", 0x80C71F);
		for (String name : TINTS.keySet()) {
			TINTED.put(name, new PokebookBlock(settings()));
		}
	}

	private ModBlocks() {
	}

	/** Cada bloco precisa das suas próprias Settings: o builder não é reutilizável. */
	private static AbstractBlock.Settings settings() {
		return AbstractBlock.Settings.create()
			.strength(1.5f)
			.sounds(BlockSoundGroup.METAL)
			// O modelo não preenche o cubo: sem isto, as faces dos blocos vizinhos somem.
			.nonOpaque()
			.luminance(state -> PokebookBlock.lightFor(state.get(PokebookBlock.SCREEN)));
	}

	public static void register() {
		register("pokebook", POKEBOOK);
		TINTED.forEach((name, block) -> register("pokebook_" + name, block));
	}

	private static void register(String path, Block block) {
		Identifier id = Identifier.of(Pokebook.MOD_ID, path);
		Registry.register(Registries.BLOCK, id, block);
		Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
	}
}
