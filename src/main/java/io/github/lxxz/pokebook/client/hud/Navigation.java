package io.github.lxxz.pokebook.client.hud;

import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.SharedLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Para onde a seta do HUD aponta.
 *
 * <p>Um alvo só de cada vez, porque é uma seta só. Três tipos, que diferem em <b>de onde
 * vem a posição</b>:
 * <ul>
 *   <li>{@link Fixed} — um lugar parado: ponto de interesse, local de morte, coordenadas
 *       clicadas no chat;</li>
 *   <li>{@link FollowPlayer} — um contato que compartilha a localização; a posição chega do
 *       servidor uma vez por segundo;</li>
 *   <li>{@link FollowEntity} — algo que o cliente já enxerga, como um Pokémon do radar; a
 *       posição é lida da entidade a cada quadro.</li>
 * </ul>
 *
 * <p>Estado só do cliente. O servidor não precisa saber para onde a seta de alguém aponta,
 * e guardar isso lá seria um pacote a cada clique para nada.
 */
public final class Navigation {
	/** Chegou: a seta some sozinha a esta distância, em blocos. */
	public static final double ARRIVAL_DISTANCE = 4.0;

	public sealed interface Target permits Fixed, FollowPlayer, FollowEntity {
		String label();
	}

	public record Fixed(GlobalPos pos, String label) implements Target {
	}

	public record FollowPlayer(UUID uuid, String label) implements Target {
	}

	public record FollowEntity(int entityId, String label) implements Target {
	}

	/**
	 * Onde o alvo está agora, se se sabe.
	 *
	 * @param dimension em que mundo — a seta só aponta se for o mesmo do jogador
	 */
	public record Resolved(RegistryKey<World> dimension, Vec3d pos) {
	}

	private static Target current;

	private Navigation() {
	}

	public static Optional<Target> current() {
		return Optional.ofNullable(current);
	}

	public static void navigateTo(Target target) {
		current = target;
	}

	public static void stop() {
		current = null;
	}

	/**
	 * A posição atual do alvo, ou vazio se ela não é conhecida neste momento.
	 *
	 * <p>Vazio não apaga o alvo: um contato que entrou num túnel e sumiu da lista por um
	 * segundo volta no seguinte. O HUD mostra "sem sinal" e a seta volta sozinha.
	 */
	public static Optional<Resolved> resolve(MinecraftClient client) {
		if (current == null || client.world == null) {
			return Optional.empty();
		}
		return switch (current) {
			case Fixed fixed -> Optional.of(new Resolved(fixed.pos().dimension(), Vec3d.ofCenter(fixed.pos().pos())));
			case FollowPlayer follow -> ClientPhone.sharedLocationOf(follow.uuid())
				.map(SharedLocation::pos)
				.map(pos -> new Resolved(pos.dimension(), Vec3d.ofCenter(pos.pos())));
			case FollowEntity follow -> {
				Entity entity = client.world.getEntityById(follow.entityId());
				yield entity == null || entity.isRemoved()
					? Optional.empty()
					: Optional.of(new Resolved(client.world.getRegistryKey(), entity.getPos()));
			}
		};
	}
}
