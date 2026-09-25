package io.github.lxxz.pokebook.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.GlobalPos;

import java.util.UUID;

/**
 * Onde está um contato que escolheu mostrar a localização para você.
 *
 * <p>Precisa vir do servidor porque o cliente não sabe onde está quem está longe: ele só
 * recebe entidades num raio em volta de si. É exatamente por isso que isto é uma
 * funcionalidade, e não algo que o jogo já dá.
 */
public record SharedLocation(UUID uuid, String name, GlobalPos pos) {
	public static final PacketCodec<RegistryByteBuf, SharedLocation> CODEC = PacketCodec.tuple(
		Uuids.PACKET_CODEC, SharedLocation::uuid,
		PacketCodecs.STRING, SharedLocation::name,
		GlobalPos.PACKET_CODEC, SharedLocation::pos,
		SharedLocation::new
	);
}
