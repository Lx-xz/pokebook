package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Servidor → cliente: manda abrir a tela do pokébook.
 *
 * <p>Por enquanto a carga útil é só o apelido do jogador — o suficiente para provar que
 * o caminho inteiro funciona. O nick vem do servidor de propósito: o cliente já sabe o
 * próprio nome, então pegá-lo localmente não testaria nada.
 *
 * <p>A posição do bloco viaja junto porque o cliente precisa dela para dois fins: medir
 * a distância enquanto a tela está aberta e dizer ao servidor qual pokébook foi fechado.
 */
public record OpenPokebookPayload(BlockPos pos, String nick) implements CustomPayload {
	public static final CustomPayload.Id<OpenPokebookPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "open"));

	public static final PacketCodec<RegistryByteBuf, OpenPokebookPayload> CODEC =
		PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenPokebookPayload::pos,
			PacketCodecs.STRING, OpenPokebookPayload::nick,
			OpenPokebookPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
