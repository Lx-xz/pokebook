package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.hud.Navigation;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.ShareLocationPayload;
import io.github.lxxz.pokebook.phone.PhoneData;
import io.github.lxxz.pokebook.phone.Waypoint;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pontos de interesse: lugares salvos, com nome e ícone, para onde a seta do HUD leva.
 *
 * <p>No topo, sempre que houver, o <b>local da última morte</b> — um ponto que ninguém
 * precisou marcar. Ele não é guardado por nós: o próprio jogo já lembra onde o jogador
 * morreu (é o que a bússola de recuperação usa) e manda essa posição ao cliente. Aqui só
 * se mostra, sem custar a bússola nem o fragmento de eco que ela pede.
 *
 * <p>Tocar num ponto abre a edição, e de lá se navega; o local de morte, que não se edita,
 * liga a seta direto.
 */
public class WaypointsScreen extends ScrollListScreen<WaypointsScreen.Row> {
	private static final int ICON = 16;

	public sealed interface Row permits Death, Saved {
	}

	public record Death(GlobalPos pos) implements Row {
	}

	/** @param index a posição na lista do servidor — é assim que ele sabe qual editar */
	public record Saved(int index, Waypoint waypoint) implements Row {
	}

	public WaypointsScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.waypoints"), session);
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new PokebookMenuScreen(session, ClientPhone.missions());
	}

	@Override
	protected int footerRows() {
		return 1;
	}

	@Override
	protected void initPanel() {
		List<ButtonWidget.Builder> buttons = new ArrayList<>();
		buttons.add(ButtonWidget.builder(Text.translatable("screen.pokebook.waypoints.mark_here"),
			button -> markHere()));
		if (ClientPhone.features().chatLocation()) {
			buttons.add(ButtonWidget.builder(Text.translatable("screen.pokebook.waypoints.share_here"),
				button -> ClientPlayNetworking.send(new ShareLocationPayload(Optional.empty()))));
		}
		if (Navigation.current().isPresent()) {
			buttons.add(ButtonWidget.builder(Text.translatable("screen.pokebook.nav.stop"), button -> {
				Navigation.stop();
				clearAndInit();
			}));
		}
		addFooterRow(0, buttons.toArray(ButtonWidget.Builder[]::new));
	}

	@Override
	protected Text emptyText() {
		return Text.translatable("screen.pokebook.waypoints.empty");
	}

	@Override
	protected List<Row> rows() {
		List<Row> rows = new ArrayList<>();
		if (client != null && client.player != null) {
			client.player.getLastDeathPos().ifPresent(pos -> rows.add(new Death(pos)));
		}
		List<Waypoint> waypoints = ClientPhone.data().waypoints();
		for (int i = 0; i < waypoints.size(); i++) {
			rows.add(new Saved(i, waypoints.get(i)));
		}
		return rows;
	}

	/** Abre a edição de um ponto novo, já com a posição e a dimensão de agora. */
	private void markHere() {
		if (client == null || client.player == null || client.world == null) {
			return;
		}
		if (ClientPhone.data().waypoints().size() >= PhoneData.MAX_WAYPOINTS) {
			client.player.sendMessage(Text.translatable("message.pokebook.waypoints.full", PhoneData.MAX_WAYPOINTS), true);
			return;
		}
		String name = Text.translatable("screen.pokebook.waypoints.default_name",
			ClientPhone.data().waypoints().size() + 1).getString();
		Waypoint fresh = new Waypoint(name,
			GlobalPos.create(client.world.getRegistryKey(), client.player.getBlockPos()),
			Waypoint.defaultIcon());
		navigateTo(new WaypointEditScreen(session, -1, fresh));
	}

	// ------------------------------------------------------------------ desenho

	@Override
	protected void renderRow(DrawContext context, Row row, int x, int y, int width, boolean hovered) {
		int textY = y + (ROW_HEIGHT - textRenderer.fontHeight) / 2;
		int iconY = y + (ROW_HEIGHT - ICON) / 2;

		// Iniciadas mesmo sendo o switch exaustivo: assim não se depende da regra de
		// "definitivamente atribuída" depois de um switch de padrões, das mais sutis do Java.
		ItemStack icon = ItemStack.EMPTY;
		String name = "";
		GlobalPos pos = null;
		int nameColor = COLOR_TEXT;
		switch (row) {
			case Death death -> {
				icon = new ItemStack(Items.RECOVERY_COMPASS);
				name = Text.translatable("screen.pokebook.waypoints.last_death").getString();
				pos = death.pos();
				nameColor = COLOR_MUTED;
			}
			case Saved saved -> {
				icon = new ItemStack(saved.waypoint().iconItem());
				name = saved.waypoint().name();
				pos = saved.waypoint().pos();
				nameColor = hovered ? COLOR_ACCENT : COLOR_TEXT;
			}
		}

		context.drawItem(icon, x, iconY);

		int where = rightAligned(context, distanceText(pos), x, y, width, COLOR_MUTED);
		int nameX = x + ICON + 4;
		context.drawText(textRenderer, textRenderer.trimToWidth(name, where - 4 - nameX), nameX, textY, nameColor, false);
	}

	/**
	 * A distância, se o ponto está no mesmo mundo do jogador; o nome do mundo, se não.
	 *
	 * <p>Distância entre dimensões não quer dizer nada — as coordenadas do Nether não são as
	 * do mundo principal —, então o que ajuda é saber <em>em qual</em> ele está.
	 */
	private Text distanceText(GlobalPos pos) {
		if (client == null || client.player == null || client.world == null) {
			return Text.empty();
		}
		if (!pos.dimension().equals(client.world.getRegistryKey())) {
			return dimensionName(pos.dimension());
		}
		double distance = client.player.getPos().distanceTo(Vec3d.ofCenter(pos.pos()));
		return Text.translatable("screen.pokebook.waypoints.distance", (int) Math.round(distance));
	}

	/** O nome curto de um mundo: os três do vanilla traduzidos, o de um mod pelo próprio id. */
	public static Text dimensionName(RegistryKey<World> dimension) {
		if (dimension.equals(World.OVERWORLD)) {
			return Text.translatable("dimension.pokebook.overworld");
		}
		if (dimension.equals(World.NETHER)) {
			return Text.translatable("dimension.pokebook.nether");
		}
		if (dimension.equals(World.END)) {
			return Text.translatable("dimension.pokebook.end");
		}
		return Text.literal(dimension.getValue().getPath());
	}

	// ------------------------------------------------------------------ interação

	@Override
	protected void onRowClicked(Row row, int index, double localX) {
		switch (row) {
			case Death death -> {
				Navigation.navigateTo(new Navigation.Fixed(death.pos(),
					Text.translatable("screen.pokebook.waypoints.last_death").getString()));
				close();
			}
			case Saved saved -> navigateTo(new WaypointEditScreen(session, saved.index(), saved.waypoint()));
		}
	}
}
