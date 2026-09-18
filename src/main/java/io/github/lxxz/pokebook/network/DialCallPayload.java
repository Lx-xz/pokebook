package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: quero ligar para esta pessoa.
 *
 * <p>Vai o <b>apelido</b> e não o {@code UUID} porque é o que o cliente tem sem pedir: a
 * lista de jogadores conectados que a tecla Tab mostra já está sincronizada na máquina
 * dele. Mandar uma lista nossa só para carregar identificadores seria repetir o que o
 * próprio jogo já entrega.
 *
 * <p>Que o nome possa não existir não é problema: o servidor procura, não acha e responde
 * com uma mensagem. Ele nunca confia neste campo — ver {@code CallService.dial}.
 */
public record DialCallPayload(String target) implements CustomPayload {
	public static final CustomPayload.Id<DialCallPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "dial_call"));

	public static final PacketCodec<RegistryByteBuf, DialCallPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.STRING, DialCallPayload::target,
			DialCallPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
