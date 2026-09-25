package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.phone.PhoneSettings;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Cliente → servidor: estes são os meus ajustes agora.
 *
 * <p>Vão inteiros. São três campos, e a tela de ajustes já tem o conjunto todo na mão;
 * um pacote por ajuste seria três pacotes para dizer a mesma coisa. O servidor passa o
 * que chegou por {@link PhoneSettings#sanitized()}.
 */
public record UpdateSettingsPayload(PhoneSettings settings) implements CustomPayload {
	public static final CustomPayload.Id<UpdateSettingsPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "update_settings"));

	public static final PacketCodec<RegistryByteBuf, UpdateSettingsPayload> CODEC =
		PacketCodec.tuple(
			PacketCodecs.codec(PhoneSettings.CODEC), UpdateSettingsPayload::settings,
			UpdateSettingsPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
