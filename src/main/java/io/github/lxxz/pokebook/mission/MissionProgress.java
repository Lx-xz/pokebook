package io.github.lxxz.pokebook.mission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O progresso de um jogador: quanto ele já fez de cada missão e quais já resgatou.
 *
 * <p>Guardado no próprio jogador, pela API de anexo de dados do Fabric — e não no
 * armazenamento do Cobblemon. A razão não é conveniência: a API do Cobblemon quebra
 * entre versões menores, e um update dele não pode ter o poder de corromper o histórico
 * de ninguém. Assim o acoplamento fica limitado a <em>ler</em> eventos.
 *
 * <p>Os campos do codec são opcionais para que um jogador antigo, salvo antes de uma
 * missão existir, continue carregando sem erro.
 */
public final class MissionProgress {
	public static final Codec<MissionProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.unboundedMap(Identifier.CODEC, Codec.INT)
			.optionalFieldOf("counts", Map.of()).forGetter(p -> p.counts),
		Identifier.CODEC.listOf()
			.optionalFieldOf("claimed", List.of()).forGetter(p -> List.copyOf(p.claimed))
	).apply(instance, MissionProgress::new));

	private final Map<Identifier, Integer> counts;
	private final Set<Identifier> claimed;

	private MissionProgress(Map<Identifier, Integer> counts, List<Identifier> claimed) {
		this.counts = new HashMap<>(counts);
		this.claimed = new HashSet<>(claimed);
	}

	public MissionProgress() {
		this(Map.of(), List.of());
	}

	public int count(Identifier missionId) {
		return counts.getOrDefault(missionId, 0);
	}

	/**
	 * Soma um ao progresso, parando no necessário.
	 *
	 * @return true se algo mudou — usado para só marcar o anexo como sujo quando preciso
	 */
	public boolean advance(Mission mission) {
		int current = count(mission.id());
		if (current >= mission.required()) {
			return false;
		}
		counts.put(mission.id(), current + 1);
		return true;
	}

	public boolean isComplete(Mission mission) {
		return count(mission.id()) >= mission.required();
	}

	public boolean isClaimed(Identifier missionId) {
		return claimed.contains(missionId);
	}

	public boolean markClaimed(Identifier missionId) {
		return claimed.add(missionId);
	}
}
