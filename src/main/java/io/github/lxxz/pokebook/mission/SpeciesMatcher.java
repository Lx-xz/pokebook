package io.github.lxxz.pokebook.mission;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Casa com uma espécie de Pokémon, ex.: {@code cobblemon:pidgey}.
 *
 * <p>Repare que <b>não há nenhum import do Cobblemon aqui</b>. A espécie é só um
 * identificador; quem sabe extraí-lo de um Pokémon é a classe de integração. Por isso
 * esta classe compila e carrega numa máquina sem o Cobblemon — inclusive a do trabalho.
 */
public record SpeciesMatcher(Identifier species) implements TargetMatcher {
	/**
	 * Aceita "pidgey" além de "cobblemon:pidgey": escrever o namespace em toda missão de
	 * Pokémon seria ruído, já que praticamente todas virão do Cobblemon.
	 */
	public static final Codec<Identifier> SPECIES_CODEC =
		Codec.STRING.xmap(SpeciesMatcher::parse, Identifier::toString);

	private static final String DEFAULT_NAMESPACE = "cobblemon";

	@Override
	public boolean matches(MissionTarget target) {
		return species.equals(target.species());
	}

	@Override
	public Text describe() {
		// O Cobblemon registra o nome da espécie sob esta chave. Sem ele instalado, a
		// chave não resolve e o jogador vê o texto cru — que ainda diz qual é a espécie.
		return Text.translatable(species.getNamespace() + ".species." + species.getPath() + ".name");
	}

	private static Identifier parse(String raw) {
		return raw.indexOf(':') < 0
			? Identifier.of(DEFAULT_NAMESPACE, raw)
			: Identifier.of(raw);
	}
}
