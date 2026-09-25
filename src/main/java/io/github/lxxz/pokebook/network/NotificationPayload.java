package io.github.lxxz.pokebook.network;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.notify.NotificationKind;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

/**
 * Servidor → cliente: um aviso para a central de notificações.
 *
 * <p>Título e corpo viajam como {@link Text}, não como string pronta — pelo mesmo motivo
 * das missões: o servidor monta com peças traduzíveis e cada cliente lê no próprio idioma.
 */
public record NotificationPayload(NotificationKind kind, Text title, Text body) implements CustomPayload {
	public static final CustomPayload.Id<NotificationPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Pokebook.MOD_ID, "notification"));

	public static final PacketCodec<RegistryByteBuf, NotificationPayload> CODEC =
		PacketCodec.tuple(
			NotificationKind.PACKET_CODEC, NotificationPayload::kind,
			TextCodecs.REGISTRY_PACKET_CODEC, NotificationPayload::title,
			TextCodecs.REGISTRY_PACKET_CODEC, NotificationPayload::body,
			NotificationPayload::new
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
