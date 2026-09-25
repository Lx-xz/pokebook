package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Cliente → servidor: grave ou apague uma nota.
 *
 * <p>{@code text} presente é gravar; ausente é apagar. {@code index} negativo com texto é
 * uma nota nova. Um pacote para as duas coisas porque é a mesma pergunta — "o que fica
 * nesta posição" — e a resposta "nada" é apagar.
 */
public record NoteActionPayload(int index, Optional<String> text) implements CustomPayload {
	public static final CustomPayload.Id<NoteActionPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "note_action"));

	public static final PacketCodec<RegistryByteBuf, NoteActionPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.VAR_INT, NoteActionPayload::index,
			PacketCodecs.optional(PacketCodecs.STRING), NoteActionPayload::text,
			NoteActionPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
