package io.github.lxxz.pokebook.item;

import io.github.lxxz.pokebook.mission.MissionService;
import io.github.lxxz.pokebook.network.OpenPokebookPayload;
import io.github.lxxz.pokebook.sound.PokebookSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * O poképhone: o pokébook que cabe no bolso.
 *
 * <p>Mostra as mesmas missões porque o progresso sempre foi <b>por jogador</b>, nunca por
 * bloco — não há dado novo nem sincronização a inventar. A decisão de arquitetura tomada
 * lá no começo é o que faz este item custar tão pouco.
 *
 * <p>A diferença vai no pacote: <b>sem posição de bloco</b>. É isso que faz a tela nascer
 * em pé, não fechar por distância e não avisar bloco nenhum ao sair.
 */
public class PokephoneItem extends Item {
	public PokephoneItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (user instanceof ServerPlayerEntity player) {
			ServerPlayNetworking.send(player, new OpenPokebookPayload(
				Optional.empty(),
				player.getGameProfile().getName(),
				MissionService.snapshot(player)
			));

			// Som só para quem abriu: é um aparelho na mão, não um móvel na sala. O do
			// bloco toca no mundo, para quem estiver por perto.
			SoundEvent sound = Registries.SOUND_EVENT.get(PokebookSounds.SCREEN_ON);
			if (sound != null) {
				player.playSound(sound, 0.4f, 1.2f);
			}
		}
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
	}
}
