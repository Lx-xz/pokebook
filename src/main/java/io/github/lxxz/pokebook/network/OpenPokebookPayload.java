package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * Servidor → cliente: abre o pokébook, com tudo que as telas precisam mostrar.
 *
 * <p>Manda o conteúdo inteiro de uma vez, em vez de um pacote por tela. Com este volume
 * de dados o desperdício é irrelevante, e em troca a navegação entre telas fica
 * instantânea e sem estado intermediário — ninguém vê uma lista vazia esperando resposta.
 * Se um dia houver aba pesada o bastante para doer, ela ganha o próprio pacote.
 *
 * <p>A posição viaja porque o cliente precisa dela para medir distância e para dizer qual
 * pokébook foi fechado.
 */
public record OpenPokebookPayload(Optional<BlockPos> pos, String nick, List<MissionEntry> missions) implements CustomPayload {
	public static final CustomPayload.Id<OpenPokebookPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "open"));

	public static final PacketCodec<RegistryByteBuf, OpenPokebookPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.optional(BlockPos.PACKET_CODEC), OpenPokebookPayload::pos,
			PacketCodecs.STRING, OpenPokebookPayload::nick,
			MissionEntry.CODEC.collect(PacketCodecs.toList()), OpenPokebookPayload::missions,
			OpenPokebookPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
