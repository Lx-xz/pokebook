package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Cliente → servidor: mexa neste contato.
 *
 * <p>Vai só o {@link UUID}. O nome o servidor tira do próprio jogador conectado — um nome
 * vindo do cliente poderia dizer qualquer coisa, e o contato apareceria com ele na lista.
 */
public record ContactActionPayload(Action action, UUID target) implements CustomPayload {
	public enum Action {
		/** Salva. Só vale para quem está online: é daí que sai o nome. */
		ADD,
		REMOVE,
		/** Liga ou desliga "este contato pode ver onde eu estou". */
		TOGGLE_SHARE_LOCATION;

		static final PacketCodec<ByteBuf, Action> PACKET_CODEC =
			PacketCodecs.VAR_INT.xmap(Action::byIndex, Action::ordinal);

		private static Action byIndex(int index) {
			Action[] values = values();
			// Índice desconhecido vira REMOVE: de todos, o que menos pode dar errado é
			// tirar alguém da lista — nunca expõe nada.
			return index >= 0 && index < values.length ? values[index] : REMOVE;
		}
	}

	public static final CustomPayload.Id<ContactActionPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "contact_action"));

	public static final PacketCodec<RegistryByteBuf, ContactActionPayload> CODEC =
		PacketCodec.tuple(
			Action.PACKET_CODEC, ContactActionPayload::action,
			Uuids.PACKET_CODEC, ContactActionPayload::target,
			ContactActionPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
