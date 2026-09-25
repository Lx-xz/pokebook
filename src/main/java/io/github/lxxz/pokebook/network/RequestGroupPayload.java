package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente → servidor: quem está no meu grupo? */
public record RequestGroupPayload() implements CustomPayload {
	public static final CustomPayload.Id<RequestGroupPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "request_group"));

	public static final PacketCodec<RegistryByteBuf, RequestGroupPayload> CODEC =
		PacketCodec.unit(new RequestGroupPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
