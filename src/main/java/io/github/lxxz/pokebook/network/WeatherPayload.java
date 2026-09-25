package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Servidor → cliente: a previsão do tempo do mundo principal.
 *
 * <p>Existe porque o cliente sabe se está chovendo <em>agora</em>, mas não quando isso vai
 * mudar: os contadores de chuva e trovoada moram nas propriedades do mundo do servidor e
 * nunca são sincronizados. É o único dado do relógio que precisa de pedido.
 *
 * <p>{@code ticksUntilChange} é quanto falta para a situação atual virar — de tempo bom
 * para chuva, ou de chuva para tempo bom. Zero ou menos quer dizer "sem previsão", que é o
 * caso de {@code /weather} ter fixado o tempo.
 */
public record WeatherPayload(boolean raining, boolean thundering, int ticksUntilChange) implements CustomPayload {
	public static final CustomPayload.Id<WeatherPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "weather"));

	public static final PacketCodec<RegistryByteBuf, WeatherPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.BOOL, WeatherPayload::raining,
			PacketCodecs.BOOL, WeatherPayload::thundering,
			PacketCodecs.VAR_INT, WeatherPayload::ticksUntilChange,
			WeatherPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
