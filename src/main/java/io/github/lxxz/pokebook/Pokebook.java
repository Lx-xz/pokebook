package io.github.lxxz.pokebook;

import io.github.lxxz.pokebook.registry.ModBlocks;
import io.github.lxxz.pokebook.registry.ModItemGroups;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pokebook implements ModInitializer {
	public static final String MOD_ID = "pokebook";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// O bloco e o item precisam existir antes da aba do criativo, que os usa como ícone e entrada.
		ModBlocks.register();
		ModItemGroups.register();
		LOGGER.info("Pokébook carregado.");
	}
}
