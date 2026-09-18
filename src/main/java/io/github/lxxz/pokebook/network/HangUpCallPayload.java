package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: me tire desta ligação.
 *
 * <p>Um pacote só para os três gestos — recusar enquanto toca, desistir enquanto chama,
 * desligar em ligação. São a mesma frase dita em momentos diferentes, e o servidor já sabe
 * em qual deles o jogador está. Três pacotes obrigariam a conferir se o cliente escolheu o
 * nome certo para o próprio estado, o que é trabalho para não ganhar nada.
 *
 * <p>Quem precisa da distinção é o <b>outro lado</b>, e ela aparece só na mensagem que ele
 * recebe — ver {@code CallService.hangUp}.
 */
public record HangUpCallPayload() implements CustomPayload {
	public static final CustomPayload.Id<HangUpCallPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "hang_up_call"));

	public static final PacketCodec<RegistryByteBuf, HangUpCallPayload> CODEC =
		PacketCodec.unit(new HangUpCallPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
