package io.github.lxxz.pokebook.mission;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * As missões que existem.
 *
 * <p>Todas usam entidades do vanilla de propósito. Isso não é andaime de teste: é o que
 * mantém o sistema inteiro verificável sem o Cobblemon instalado — inclusive no dia em
 * que um update do Cobblemon quebrar a integração e for preciso saber de quem é o defeito.
 *
 * <p>São sempre ativas: não há aceitar nem recusar, o progresso conta sozinho.
 */
public final class Missions {
	private static final Map<Identifier, Mission> BY_ID = new LinkedHashMap<>();

	public static final Mission THREE_COWS = register(new Mission(
		id("three_cows"), ObjectiveType.KILL,
		new EntityTypeMatcher(EntityType.COW), 3, Items.LEATHER, 8));

	public static final Mission FIVE_ZOMBIES = register(new Mission(
		id("five_zombies"), ObjectiveType.KILL,
		new EntityTypeMatcher(EntityType.ZOMBIE), 5, Items.IRON_INGOT, 3));

	public static final Mission TEN_CHICKENS = register(new Mission(
		id("ten_chickens"), ObjectiveType.KILL,
		new EntityTypeMatcher(EntityType.CHICKEN), 10, Items.GOLDEN_APPLE, 1));

	private Missions() {
	}

	private static Identifier id(String path) {
		return Identifier.of(Pokebook.MOD_ID, path);
	}

	private static Mission register(Mission mission) {
		BY_ID.put(mission.id(), mission);
		return mission;
	}

	/** Na ordem de declaração — é a ordem em que aparecem na interface. */
	public static Iterable<Mission> all() {
		return BY_ID.values();
	}

	public static Mission byId(Identifier id) {
		return BY_ID.get(id);
	}
}
