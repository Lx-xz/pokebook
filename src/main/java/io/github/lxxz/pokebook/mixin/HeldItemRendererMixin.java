package io.github.lxxz.pokebook.mixin;

import io.github.lxxz.pokebook.client.screen.PokebookScreenBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Some a mão e o item enquanto o poképhone está aberto.
 *
 * <p>Uma tela aberta no Minecraft <b>não</b> esconde a mão: o mundo continua sendo
 * desenhado atrás dela, braço incluído. Segurando o celular na frente do rosto e com a
 * interface dele ocupando a tela, o braço fica sobrando na cena.
 *
 * <p><b>Por que isto é mixin e não código normal:</b> quem desenha a mão é o próprio
 * jogo, num método que nenhuma API oferece gancho para cancelar. Mixin é a técnica que o
 * Fabric usa para reescrever um método do jogo em tempo de carga — aqui, para desistir do
 * desenho logo no começo, quando a condição vale. É a primeira do projeto; até hoje tudo
 * coube em API pública.
 *
 * <p>O descritor do método vai escrito por inteiro porque {@code renderItem} tem duas
 * sobrecargas, e só esta desenha a primeira pessoa. Os nomes aqui são Yarn; o
 * processador de anotações do Loom os traduz na compilação.
 *
 * <p>Cancelar aqui apaga <b>mão e item juntos</b>, que é exatamente o pedido: sem celular
 * e sem mão.
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
	@Inject(
		method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;"
			+ "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"
			+ "Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void pokebook$hideHandsWhilePhoneIsOpen(float tickDelta, MatrixStack matrices,
			VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light,
			CallbackInfo info) {
		// Só o aparelho de bolso esconde a mão. Com o pokébook, a tela é do bloco à sua
		// frente e a mão não está no caminho de nada.
		if (MinecraftClient.getInstance().currentScreen instanceof PokebookScreenBase screen
			&& screen.hidesHands()) {
			info.cancel();
		}
	}
}
