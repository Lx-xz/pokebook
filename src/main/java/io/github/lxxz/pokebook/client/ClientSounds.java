package io.github.lxxz.pokebook.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Som de interface, tocado só neste cliente e sem posição no mundo.
 *
 * <p>Vive no pacote de cliente, e não em {@code PokebookSounds}, porque usa classes de
 * {@code net.minecraft.client} — e aquele arquivo é carregado também no servidor dedicado.
 *
 * <p>Mesma regra de {@code PokebookSounds}: procura por id e fica em silêncio se o som não
 * existir.
 */
public final class ClientSounds {
	private ClientSounds() {
	}

	public static void play(Identifier soundId, float volume, float pitch) {
		SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
		if (sound != null) {
			// ⚠️ Em master() o TOM vem antes do volume. Os dois são float, então trocar a
			// ordem compila e toca um som esganiçado ou mudo, sem erro nenhum.
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
		}
	}
}
