package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: quero a previsão do tempo.
 *
 * <p>Sob demanda, como a aba social: só quem abre o relógio paga pela resposta.
 */
public record RequestWeatherPayload() implements CustomPayload {
	public static final CustomPayload.Id<RequestWeatherPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "request_weather"));

	public static final PacketCodec<RegistryByteBuf, RequestWeatherPayload> CODEC =
		PacketCodec.unit(new RequestWeatherPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
