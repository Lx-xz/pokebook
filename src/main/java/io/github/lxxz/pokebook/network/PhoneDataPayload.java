package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.phone.PhoneData;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Servidor → cliente: os dados do seu aparelho, inteiros.
 *
 * <p>Sai ao entrar no mundo e depois de cada mudança. Inteiro, e não só o que mudou,
 * porque é pouco dado e um protocolo de diferenças teria de ser escrito, testado e
 * depurado nos dois lados — ver {@link PhoneData}.
 *
 * <p>O codec é o <b>mesmo</b> que grava no disco. Um campo novo no {@code PhoneData}
 * chega ao cliente sem ninguém lembrar de mexer aqui.
 */
public record PhoneDataPayload(PhoneData data) implements CustomPayload {
	public static final CustomPayload.Id<PhoneDataPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "phone_data"));

	public static final PacketCodec<RegistryByteBuf, PhoneDataPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.registryCodec(PhoneData.CODEC), PhoneDataPayload::data,
			PhoneDataPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
