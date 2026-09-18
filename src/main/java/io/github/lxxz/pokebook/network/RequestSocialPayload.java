package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: quero ver a aba social.
 *
 * <p>Não carrega nada. O servidor sabe quem pediu pela conexão, e o que ele devolve não
 * depende de mais nada.
 *
 * <p>É pedido sob demanda, e não enviado junto ao abrir o pokébook, porque a maioria das
 * aberturas nunca chega a esta aba. Mandar a lista de todo mundo em toda abertura seria
 * pagar sempre por algo usado às vezes.
 */
public record RequestSocialPayload() implements CustomPayload {
	public static final CustomPayload.Id<RequestSocialPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "request_social"));

	public static final PacketCodec<RegistryByteBuf, RequestSocialPayload> CODEC =
		PacketCodec.unit(new RequestSocialPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
