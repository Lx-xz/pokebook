package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Servidor → cliente: onde estão, agora, os contatos que compartilham a localização com
 * você.
 *
 * <p>Repetido uma vez por segundo enquanto a lista não estiver vazia, e uma última vez
 * vazia quando o último parar — é o que apaga a seta de quem deixou de compartilhar.
 */
public record SharedLocationsPayload(List<SharedLocation> locations) implements CustomPayload {
	public static final CustomPayload.Id<SharedLocationsPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "shared_locations"));

	public static final PacketCodec<RegistryByteBuf, SharedLocationsPayload> CODEC =
		PacketCodec.tuple(
			SharedLocation.CODEC.collect(PacketCodecs.toList()), SharedLocationsPayload::locations,
			SharedLocationsPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
