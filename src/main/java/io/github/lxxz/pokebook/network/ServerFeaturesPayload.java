package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.config.ServerFeatures;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Servidor → cliente: o que está ligado neste servidor.
 *
 * <p>Sai uma vez, ao entrar. Serve só para o cliente não oferecer o que o servidor
 * desligou — ver {@link ServerFeatures}.
 */
public record ServerFeaturesPayload(ServerFeatures features) implements CustomPayload {
	public static final CustomPayload.Id<ServerFeaturesPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "server_features"));

	public static final PacketCodec<RegistryByteBuf, ServerFeaturesPayload> CODEC =
		PacketCodec.tuple(
			ServerFeatures.PACKET_CODEC, ServerFeaturesPayload::features,
			ServerFeaturesPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
