package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: tapa ou destapa o próprio microfone na ligação atual.
 *
 * <p>Toggle, não um booleano — o mesmo padrão do {@link HangUpCallPayload}. Quem decide o
 * novo estado é o servidor ({@code CallService.toggleMute}), e ele avisa de volta pelo
 * {@link CallStatePayload} de sempre; o cliente não guarda opinião própria sobre se está
 * mudo.
 */
public record MuteCallPayload() implements CustomPayload {
	public static final CustomPayload.Id<MuteCallPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "mute_call"));

	public static final PacketCodec<RegistryByteBuf, MuteCallPayload> CODEC =
		PacketCodec.unit(new MuteCallPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
