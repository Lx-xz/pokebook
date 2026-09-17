package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Cliente → servidor: avisa que a tela foi fechada.
 *
 * <p>Este pacote existe porque escolhemos {@code Screen} + pacote próprio em vez de
 * {@code ScreenHandler}. O servidor não tem como saber que o jogador apertou ESC — a
 * tela é puramente do cliente. Sem este aviso, a luz do bloco (que é estado de
 * servidor) ficaria acesa para sempre.
 */
public record ClosePokebookPayload(BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<ClosePokebookPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "close"));

	public static final PacketCodec<RegistryByteBuf, ClosePokebookPayload> CODEC =
		PacketCodec.tuple(
			BlockPos.PACKET_CODEC, ClosePokebookPayload::pos,
			ClosePokebookPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
