package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.call.CallState;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Servidor → cliente: em que pé está a sua ligação.
 *
 * <p>Sai a cada transição, e não de tempos em tempos: são quatro estados e eles só mudam
 * quando alguém faz alguma coisa. O aviso acima da hotbar é que se repete — mas esse é
 * texto pronto montado no servidor, não estado.
 *
 * <p>O nome do outro vai junto porque é o que a tela mostra, e ele pode desconectar entre
 * uma coisa e outra. Vem vazio quando não há ligação — {@link CallState#IDLE} não tem par.
 *
 * <p>{@code muted} é se <b>este</b> jogador tapou o próprio microfone — o servidor que
 * decide, não um estado só de cliente, porque é ele quem sabe se o pacote de áudio está
 * sendo mesmo repassado.
 */
public record CallStatePayload(CallState state, String peer, boolean muted) implements CustomPayload {
	public static final CustomPayload.Id<CallStatePayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "call_state"));

	public static final PacketCodec<RegistryByteBuf, CallStatePayload> CODEC =
		PacketCodec.tuple(
			CallState.PACKET_CODEC, CallStatePayload::state,
			PacketCodecs.STRING, CallStatePayload::peer,
			PacketCodecs.BOOL, CallStatePayload::muted,
			CallStatePayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
