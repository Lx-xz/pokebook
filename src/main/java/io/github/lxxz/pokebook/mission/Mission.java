package io.github.lxxz.pokebook.mission;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Uma missão: um objetivo, um alvo, uma quantidade e uma recompensa.
 *
 * <p>Definida em Java e não em datapack — por enquanto. A forma aqui já é a que um JSON
 * teria, então extrair depois é escrever o carregador, não redesenhar o modelo. Fazer o
 * contrário (schema primeiro, missões depois) seria adivinhar quais campos existem.
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
	int rewardCount
) {
	/** Chave de tradução: {@code mission.pokebook.<id>}. */
	public Text title() {
		return titleOf(id);
	}

	/**
	 * O mesmo título a partir do id sozinho.
	 *
	 * <p>O cliente recebe só o id pela rede e monta a chave aqui, para que cada jogador
	 * leia no próprio idioma em vez de no idioma do servidor.
	 */
	public static Text titleOf(Identifier id) {
		return Text.translatable("mission." + id.getNamespace() + "." + id.getPath());
	}

	public ItemStack reward() {
		return new ItemStack(rewardItem, rewardCount);
	}
}
