package io.github.lxxz.pokebook.sound;

import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Os sons do pokébook, emprestados do Cobblemon.
 *
 * <p>Por que emprestar: o pokébook <em>é</em> um computador, e o Cobblemon já tem os sons
 * de um. Fazer os nossos seria refazer o que já existe, e o destino do mod é conviver com
 * ele de qualquer forma.
 *
 * <p><b>Como, e por que assim:</b> os sons são procurados no registro <em>por id</em>, em
 * tempo de execução. Nenhum arquivo do Cobblemon é copiado para o nosso jar — copiar
 * seria redistribuir asset alheio; referenciar por id não é. E como é busca no registro,
 * nenhuma classe do Cobblemon é mencionada: isto compila e roda numa instalação sem ele.
 *
 * <p>Sem o Cobblemon a busca devolve {@code null} e o mod simplesmente fica em silêncio.
 * Um pokébook mudo é um defeito pequeno; um pokébook que derruba o jogo não.
 */
public final class PokebookSounds {
	/** Tela acendendo. */
	public static final Identifier SCREEN_ON = Identifier.of("cobblemon", "pc.on");

	/** Tela apagando. */
	public static final Identifier SCREEN_OFF = Identifier.of("cobblemon", "pc.off");

	/** Navegação entre telas da interface. */
	public static final Identifier PAGE = Identifier.of("cobblemon", "gui.click");

	/**
	 * O toque da ligação.
	 *
	 * <p>Este é do <b>vanilla</b>, não do Cobblemon, e de propósito: o toque é o único som
	 * daqui que o jogador <em>precisa</em> ouvir — perdê-lo é perder a ligação, não perder
	 * um detalhe. Um sino existe em qualquer instalação; um som emprestado, não.
	 */
	public static final Identifier RING = Identifier.of("minecraft", "block.note_block.bell");

	private PokebookSounds() {
	}

	/** Toca no mundo, para quem estiver por perto. Silencioso se o som não existir. */
	public static void playAt(World world, BlockPos pos, Identifier soundId, float volume, float pitch) {
		SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
		if (sound != null) {
			world.playSound(null, pos, sound, SoundCategory.BLOCKS, volume, pitch);
		}
	}

	/**
	 * Toca só para este jogador, onde quer que ele esteja.
	 *
	 * <p>É o que uma ligação precisa: o telefone toca <b>no bolso de quem é chamado</b>, e
	 * não num ponto do mundo. Quem está ao lado não tem por que ouvir o aparelho dos
	 * outros.
	 */
	public static void playTo(ServerPlayerEntity player, Identifier soundId, float volume, float pitch) {
		SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
		if (sound != null) {
			player.playSound(sound, volume, pitch);
		}
	}
}
