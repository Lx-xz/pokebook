package io.github.lxxz.pokebook.client.hud;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.client.clock.Alarms;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.MissionEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Tudo o que o mod desenha na tela durante o jogo, num lugar só.
 *
 * <p><b>Um dono único</b> é o que impede as coisas de se sobreporem. A missão acompanhada, a
 * seta e o timer são empilhados de cima para baixo no canto superior esquerdo — o único
 * canto que o vanilla deixa vazio —, e cada um desenha a partir de onde o anterior parou.
 * Se cada funcionalidade registrasse o próprio gancho de HUD, cada uma escolheria uma
 * posição sem saber das outras.
 *
 * <p>Só aparece com o poképhone no inventário: é o aparelho que mostra isto, e sem ele não
 * há o que mostrar. E some com o F3 aberto, que ocupa o mesmo canto.
 *
 * <p>Registrado por {@code HudRenderCallback}, que é chamado pelo próprio HUD do jogo — e
 * por isso já some sozinho com o F1.
 */
public final class PokebookHud {
	private static final Identifier ARROW = Identifier.of(Pokebook.MOD_ID, "seta");
	private static final int ARROW_SIZE = 14;

	private static final int MARGIN = 4;
	private static final int PADDING = 2;
	private static final int ROW_GAP = 3;

	private static final int PANEL = 0x80000000;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFB0B0B0;
	private static final int DONE = 0xFF7CE07C;

	private PokebookHud() {
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.getDebugHud().shouldShowDebugHud() || !ClientPhone.carriesPhone()) {
			return;
		}

		int y = MARGIN;
		y = renderTrackedMission(context, client.textRenderer, y);
		y = renderNavigation(context, client, y);
		renderTimer(context, client.textRenderer, y);
	}

	/** A missão acompanhada: título e contagem, ou o aviso de que dá para resgatar. */
	private static int renderTrackedMission(DrawContext context, TextRenderer textRenderer, int y) {
		Optional<MissionEntry> tracked = ClientPhone.trackedMission();
		if (tracked.isEmpty()) {
			return y;
		}
		MissionEntry mission = tracked.get();

		Text line = mission.complete()
			? Text.translatable("hud.pokebook.mission.ready", mission.title())
			: Text.translatable("hud.pokebook.mission.progress", mission.title(), mission.count(), mission.required());
		return row(context, textRenderer, line, mission.complete() ? DONE : TEXT, 0, y);
	}

	/**
	 * A seta, o nome do alvo e a distância.
	 *
	 * <p>A conta do ângulo, porque ela confunde: no Minecraft o {@code yaw} 0 olha para o sul
	 * (+Z) e cresce girando para a direita — 90 é oeste, 180 é norte. O ângulo do alvo sai de
	 * {@code atan2(-dx, dz)} na mesma convenção, e a diferença entre os dois, embrulhada em
	 * ±180, é o quanto virar: zero é em frente, negativo é à esquerda.
	 *
	 * <p>O desenho da seta aponta para a <b>esquerda</b> (é o botão de voltar), então somam-se
	 * 90° para ela apontar para cima quando o alvo está em frente. Na tela o y cresce para
	 * baixo, e por isso a rotação positiva em Z aparece no sentido horário: esquerda + 90° é
	 * para cima, que é o que se quer.
	 */
	private static int renderNavigation(DrawContext context, MinecraftClient client, int y) {
		Optional<Navigation.Target> target = Navigation.current();
		if (target.isEmpty()) {
			return y;
		}
		String label = target.get().label();
		Optional<Navigation.Resolved> resolved = Navigation.resolve(client);

		if (resolved.isEmpty()) {
			return row(context, client.textRenderer,
				Text.translatable("hud.pokebook.nav.no_signal", label), MUTED, 0, y);
		}
		if (!resolved.get().dimension().equals(client.world.getRegistryKey())) {
			return row(context, client.textRenderer,
				Text.translatable("hud.pokebook.nav.other_dimension", label), MUTED, 0, y);
		}

		Vec3d player = client.player.getPos();
		Vec3d goal = resolved.get().pos();
		double distance = player.distanceTo(goal);

		// Chegou num lugar parado: a seta cumpriu o papel e sai sozinha. Quem segue alguém
		// continua seguindo — chegar perto de quem anda não é chegar.
		if (distance < Navigation.ARRIVAL_DISTANCE && target.get() instanceof Navigation.Fixed) {
			Navigation.stop();
			return y;
		}

		double dx = goal.x - player.x;
		double dz = goal.z - player.z;
		float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float relative = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());

		Text line = Text.translatable("hud.pokebook.nav.distance", label, (int) Math.round(distance));
		int textWidth = client.textRenderer.getWidth(line);
		int height = ARROW_SIZE;
		int width = ARROW_SIZE + 3 + textWidth;

		context.fill(MARGIN - PADDING, y - PADDING, MARGIN + width + PADDING, y + height + PADDING, PANEL);

		context.getMatrices().push();
		context.getMatrices().translate(MARGIN + ARROW_SIZE / 2f, y + ARROW_SIZE / 2f, 0f);
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f + relative));
		context.drawGuiTexture(ARROW, -ARROW_SIZE / 2, -ARROW_SIZE / 2, ARROW_SIZE, ARROW_SIZE);
		context.getMatrices().pop();

		context.drawText(client.textRenderer, line, MARGIN + ARROW_SIZE + 3,
			y + (ARROW_SIZE - client.textRenderer.fontHeight) / 2 + 1, TEXT, true);

		return y + height + PADDING * 2 + ROW_GAP;
	}

	private static int renderTimer(DrawContext context, TextRenderer textRenderer, int y) {
		OptionalLong remaining = Alarms.timerRemaining();
		if (remaining.isEmpty()) {
			return y;
		}
		long seconds = (remaining.getAsLong() + 999) / 1000;
		Text line = Text.translatable("hud.pokebook.timer", "%02d:%02d".formatted(seconds / 60, seconds % 60));
		return row(context, textRenderer, line, TEXT, 0, y);
	}

	/** Uma linha de texto com fundo translúcido, como o chat. Devolve onde a próxima começa. */
	private static int row(DrawContext context, TextRenderer textRenderer, Text text, int color, int indent, int y) {
		int x = MARGIN + indent;
		int width = textRenderer.getWidth(text);
		context.fill(x - PADDING, y - PADDING, x + width + PADDING, y + textRenderer.fontHeight + PADDING, PANEL);
		context.drawText(textRenderer, text, x, y, color, true);
		return y + textRenderer.fontHeight + PADDING * 2 + ROW_GAP;
	}
}
