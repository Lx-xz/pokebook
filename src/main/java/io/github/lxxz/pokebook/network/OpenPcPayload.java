package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Cliente → servidor: abra o meu PC do Cobblemon, a partir deste pokébook.
 *
 * <p>A posição vai junto porque o acesso vale enquanto o jogador estiver <b>perto deste
 * bloco</b> — é o que faz o PC remoto ser coisa de estação, e não de bolso. O servidor
 * confere que ele está de fato com este pokébook aberto.
 *
 * <p>O pacote em si não menciona o Cobblemon; quem o atende é a integração, e só existe
 * quem atenda com o mod instalado.
 */
public record OpenPcPayload(BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<OpenPcPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "open_pc"));

	public static final PacketCodec<RegistryByteBuf, OpenPcPayload> CODEC =
		PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenPcPayload::pos,
			OpenPcPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
