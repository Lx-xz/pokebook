package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * A lanterna, nos dois sentidos. Do cliente é o pedido ("quero acesa"); do servidor é o que
 * de fato ficou — que pode ser "apagada" se ele recusou ou se ela se apagou sozinha.
 */
public record FlashlightPayload(boolean on) implements CustomPayload {
	public static final CustomPayload.Id<FlashlightPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "flashlight"));

	public static final PacketCodec<RegistryByteBuf, FlashlightPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.BOOL, FlashlightPayload::on,
			FlashlightPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
