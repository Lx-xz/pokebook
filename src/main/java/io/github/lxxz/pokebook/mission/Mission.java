package io.github.lxxz.pokebook.mission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Uma missão: um objetivo, um alvo, uma quantidade e uma recompensa.
 *
 * <p>Carregada de datapack. O modelo nasceu em Java e só depois virou JSON — o contrário
 * (schema primeiro, missões depois) seria adivinhar quais campos existem.
 *
 * <p>A recompensa é item + quantidade, e não um {@link ItemStack} pronto, porque
 * ItemStack é mutável: guardar um numa constante compartilhada é convite para alguém
 * alterá-lo sem querer e contaminar todos os resgates.
 */
public record Mission(
	Identifier id,
	ObjectiveType objective,
	TargetMatcher target,
	int required,
	Item rewardItem,
	int rewardCount,
	Optional<String> titleOverride
) {
	/**
	 * O codec de um arquivo de missão.
	 *
	 * <p>O id não vem do JSON: vem do caminho do arquivo, como em toda pasta de datapack
	 * do vanilla. Por isso ele é capturado aqui em vez de virar campo — assim não há como
	 * um arquivo declarar um id diferente do nome dele.
	 *
	 * <p>Só {@code target} é obrigatório. O resto tem padrão, para que a missão mais
	 * simples possível caiba em uma linha.
	 */
	public static Codec<Mission> codec(Identifier id) {
		return RecordCodecBuilder.create(instance -> instance.group(
			ObjectiveType.CODEC.optionalFieldOf("objective", ObjectiveType.KILL).forGetter(Mission::objective),
			TargetMatcher.CODEC.fieldOf("target").forGetter(Mission::target),
			Codec.INT.optionalFieldOf("required", 1).forGetter(Mission::required),
			Registries.ITEM.getCodec().optionalFieldOf("reward", net.minecraft.item.Items.AIR).forGetter(Mission::rewardItem),
			Codec.INT.optionalFieldOf("reward_count", 1).forGetter(Mission::rewardCount),
			Codec.STRING.optionalFieldOf("title").forGetter(Mission::titleOverride)
		).apply(instance, (objective, target, required, rewardItem, rewardCount, title) ->
			new Mission(id, objective, target, required, rewardItem, rewardCount, title)));
	}

	/**
	 * O nome que aparece na interface.
	 *
	 * <p>Por padrão é <b>montado</b> a partir das partes que toda missão já tem: o verbo
	 * do objetivo, o alvo e a quantidade — "Abater Zumbi ×5". Nada de chave de tradução
	 * por missão: criar uma missão nova não pode exigir editar um arquivo de idioma por
	 * língua, senão o datapack não serve para nada.
	 *
	 * <p>A quantidade vem depois do alvo, com "×", em vez de "Abater 5 zumbis". O motivo é
	 * chato mas real: os arquivos de idioma do Minecraft <b>não têm plural</b>, e o nome
	 * que o jogo dá a uma entidade é sempre singular. "Abater 5 Zumbi" sairia errado em
	 * qualquer idioma; "Zumbi ×5" sai certo em todos.
	 *
	 * <p>{@code title} no JSON sobrescreve tudo isso, para a missão que merecer um nome
	 * próprio.
	 *
	 * <p>Em todos os casos o {@link Text} é montado no servidor e viaja pronto — mas
	 * montado com peças traduzíveis, que o cliente resolve. Cada jogador segue lendo no
	 * próprio idioma.
	 */
	public Text title() {
		return titleOverride.<Text>map(Text::literal).orElseGet(this::generatedTitle);
	}

	private Text generatedTitle() {
		return required == 1
			? Text.translatable(objective.translationKey() + ".one", target.describe())
			: Text.translatable(objective.translationKey(), target.describe(), required);
	}

	public ItemStack reward() {
		return new ItemStack(rewardItem, rewardCount);
	}

	public boolean hasReward() {
		return rewardItem != net.minecraft.item.Items.AIR && rewardCount > 0;
	}
}
