package io.github.lxxz.pokebook.mission;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Em quem a missão vale.
 *
 * <p>Este é um dos dois eixos de extensão do sistema; o outro é {@link ObjectiveType}.
 * Separar os dois é o que permite combinar "capturar" com "qualquer coisa do tipo fogo"
 * sem escrever uma classe por combinação — e é por isso que cinco tipos de missão saem de
 * quatro alvos e três objetivos, em vez de exigirem quinze classes.
 */
public interface TargetMatcher {
	boolean matches(MissionTarget target);

	/** Como o alvo aparece na interface, ex.: "Vaca", "Pidgey", "Fogo", "Geração 1". */
	Text describe();

	/**
	 * As formas aceitas em JSON:
	 *
	 * <pre>
	 * "target": "minecraft:cow"           um tipo de entidade
	 * "target": { "species": "pidgey" }   uma espécie
	 * "target": { "element": "fire" }     um tipo elemental
	 * "target": { "generation": 1 }       uma geração
	 * </pre>
	 *
	 * <p>A forma de objeto <b>não</b> tem campo {@code "type"} de despacho, e isso é
	 * deliberado: o objeto é identificado pela chave que ele traz, então uma missão de
	 * espécie se escreve {@code {"species": "pidgey"}} e não
	 * {@code {"type": "pokebook:species", "species": "pidgey"}}. Com quatro formas, o
	 * ganho de brevidade em todo arquivo escrito à mão vale mais do que a generalidade de
	 * um despacho — e o custo é este codec ser escrito à mão em vez de gerado.
	 *
	 * <p>Declarar duas chaves ao mesmo tempo é <b>erro</b>, não precedência silenciosa:
	 * {@code {"species": "pidgey", "element": "fire"}} quase certamente quer dizer "Pidgey
	 * do tipo fogo", que este modelo não sabe expressar. Falhar alto é melhor do que
	 * ignorar metade do pedido em silêncio.
	 */
	Codec<TargetMatcher> CODEC = Codec.either(EntityTypeMatcher.CODEC, ObjectForm.CODEC)
		.xmap(
			either -> either.map(entity -> (TargetMatcher) entity, object -> object),
			matcher -> matcher instanceof EntityTypeMatcher entityType
				? Either.left(entityType)
				: Either.right(matcher)
		);

	/** O formato de objeto, com todas as chaves opcionais e exatamente uma exigida. */
	final class ObjectForm {
		private ObjectForm() {
		}

		private record Fields(Optional<Identifier> species, Optional<String> element, Optional<Integer> generation) {
		}

		private static final Codec<Fields> FIELDS = RecordCodecBuilder.create(instance -> instance.group(
			SpeciesMatcher.SPECIES_CODEC.optionalFieldOf("species").forGetter(Fields::species),
			Codec.STRING.optionalFieldOf("element").forGetter(Fields::element),
			Codec.INT.optionalFieldOf("generation").forGetter(Fields::generation)
		).apply(instance, Fields::new));

		static final Codec<TargetMatcher> CODEC = FIELDS.flatXmap(ObjectForm::toMatcher, ObjectForm::toFields);

		private static DataResult<TargetMatcher> toMatcher(Fields fields) {
			int declared = (fields.species().isPresent() ? 1 : 0)
				+ (fields.element().isPresent() ? 1 : 0)
				+ (fields.generation().isPresent() ? 1 : 0);

			if (declared != 1) {
				return DataResult.error(() -> declared == 0
					? "Alvo sem critério: declare species, element ou generation"
					: "Alvo com mais de um critério: declare apenas um de species, element ou generation");
			}

			if (fields.species().isPresent()) {
				return DataResult.success(new SpeciesMatcher(fields.species().get()));
			}
			if (fields.element().isPresent()) {
				return DataResult.success(new ElementMatcher(fields.element().get()));
			}
			return DataResult.success(new GenerationMatcher(fields.generation().get()));
		}

		private static DataResult<Fields> toFields(TargetMatcher matcher) {
			if (matcher instanceof SpeciesMatcher species) {
				return DataResult.success(new Fields(Optional.of(species.species()), Optional.empty(), Optional.empty()));
			}
			if (matcher instanceof ElementMatcher element) {
				return DataResult.success(new Fields(Optional.empty(), Optional.of(element.element()), Optional.empty()));
			}
			if (matcher instanceof GenerationMatcher generation) {
				return DataResult.success(new Fields(Optional.empty(), Optional.empty(), Optional.of(generation.generation())));
			}
			return DataResult.error(() -> "Alvo sem forma serializável: " + matcher);
		}
	}
}
