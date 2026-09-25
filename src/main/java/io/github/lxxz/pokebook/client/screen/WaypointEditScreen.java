package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.hud.Navigation;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.ShareLocationPayload;
import io.github.lxxz.pokebook.network.WaypointActionPayload;
import io.github.lxxz.pokebook.phone.Waypoint;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Um ponto de interesse aberto: dar nome, escolher ícone, e usar.
 *
 * <p>É a primeira tela <b>neta</b> do aparelho — filha da lista, que é filha do menu. Foi
 * por ela que o botão central passou a subir até a raiz em vez de um degrau.
 *
 * <p>A tela <b>não</b> aplica a mudança por conta própria: "salvar" manda o pacote e volta
 * para a lista, e a lista mostra o que o servidor gravou quando a resposta chega. O nome
 * que o jogador digitou pode voltar diferente — aparado, cortado, sem caracteres
 * inválidos —, e é o do servidor que vale.
 *
 * <p>Ponto novo e ponto existente são a mesma tela: um ponto novo é um ponto de índice -1
 * que ainda não foi salvo, e por isso não tem "apagar".
 */
public class WaypointEditScreen extends PokebookScreenBase {
	private static final int FIELD_HEIGHT = 18;
	private static final int ICON_CELL = 22;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;
	private static final int GAP = 6;

	/** Quanto tempo "apagar" fica armado esperando o segundo clique. */
	private static final long CONFIRM_MS = 3000;

	private final int index;
	private final Waypoint original;

	/**
	 * O ícone escolhido. Sobrevive a {@code clearAndInit()} porque é campo da tela, e não do
	 * widget; o texto do nome também é guardado antes de remontar, ver {@link #initPanel()}.
	 */
	private Item icon;
	private String name;

	private TextFieldWidget nameField;
	private ButtonWidget deleteButton;
	private long deleteArmedAt = -1;

	public WaypointEditScreen(PokebookSession session, int index, Waypoint waypoint) {
		super(Text.translatable(index < 0 ? "screen.pokebook.waypoints.new" : "screen.pokebook.waypoints.edit"), session);
		this.index = index;
		this.original = waypoint;
		this.icon = waypoint.iconItem();
		this.name = waypoint.name();
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return new WaypointsScreen(session);
	}

	/** Em pé os ícones ocupam duas fileiras e os botões também; deitado, uma de cada. */
	private int iconsPerRow() {
		return session.portrait() ? 5 : Waypoint.ICONS.size();
	}

	private int fieldY() {
		return contentTop() + 2;
	}

	private int iconsY() {
		return fieldY() + FIELD_HEIGHT + GAP;
	}

	private int iconRows() {
		return (Waypoint.ICONS.size() + iconsPerRow() - 1) / iconsPerRow();
	}

	private int iconsX() {
		return contentX() + (contentWidth() - iconsPerRow() * ICON_CELL) / 2;
	}

	private int coordinatesY() {
		return iconsY() + iconRows() * ICON_CELL + GAP;
	}

	@Override
	protected void initPanel() {
		// O campo é recriado a cada remontagem; o texto digitado até aqui é guardado antes,
		// senão remontar (ao armar o "apagar", por exemplo) apagaria o que o jogador escreveu.
		if (nameField != null) {
			name = nameField.getText();
		}
		nameField = new TextFieldWidget(textRenderer, contentX(), fieldY(), contentWidth(), FIELD_HEIGHT,
			Text.translatable("screen.pokebook.waypoints.name"));
		nameField.setMaxLength(Waypoint.MAX_NAME_LENGTH);
		nameField.setText(name);
		nameField.setPlaceholder(Text.translatable("screen.pokebook.waypoints.name"));
		addDrawableChild(nameField);
		setInitialFocus(nameField);

		ButtonWidget.Builder navigate = ButtonWidget.builder(Text.translatable("screen.pokebook.nav.go"), button -> {
			Navigation.navigateTo(new Navigation.Fixed(current().pos(), current().name()));
			close();
		});
		ButtonWidget.Builder share = ButtonWidget.builder(Text.translatable("screen.pokebook.waypoints.share"),
			button -> ClientPlayNetworking.send(new ShareLocationPayload(Optional.of(current()))));
		ButtonWidget.Builder save = ButtonWidget.builder(Text.translatable("screen.pokebook.save"), button -> {
			ClientPlayNetworking.send(new WaypointActionPayload(index, Optional.of(current())));
			navigateTo(parentScreen());
		});

		boolean canShare = ClientPhone.features().chatLocation();
		boolean canDelete = index >= 0;

		if (session.portrait()) {
			row(1, canShare ? new ButtonWidget.Builder[] {navigate, share} : new ButtonWidget.Builder[] {navigate});
			row(0, canDelete ? new ButtonWidget.Builder[] {save, deleteBuilder()} : new ButtonWidget.Builder[] {save});
		} else {
			List<ButtonWidget.Builder> all = new ArrayList<>();
			all.add(navigate);
			if (canShare) {
				all.add(share);
			}
			all.add(save);
			if (canDelete) {
				all.add(deleteBuilder());
			}
			row(0, all.toArray(ButtonWidget.Builder[]::new));
		}
	}

	private ButtonWidget.Builder deleteBuilder() {
		return ButtonWidget.builder(Text.translatable("screen.pokebook.delete"), button -> {
			// Dois cliques: apagar não tem volta, e o botão fica bem ao lado de "salvar".
			if (deleteArmedAt >= 0 && System.currentTimeMillis() - deleteArmedAt < CONFIRM_MS) {
				ClientPlayNetworking.send(new WaypointActionPayload(index, Optional.empty()));
				navigateTo(parentScreen());
				return;
			}
			deleteArmedAt = System.currentTimeMillis();
			button.setMessage(Text.translatable("screen.pokebook.delete_confirm"));
		});
	}

	/** Uma fileira de botões iguais, contando de baixo. */
	private void row(int fromBottom, ButtonWidget.Builder... buttons) {
		int y = contentY() + contentHeight() - BUTTON_HEIGHT - fromBottom * (BUTTON_HEIGHT + BUTTON_GAP);
		int each = (contentWidth() - BUTTON_GAP * (buttons.length - 1)) / buttons.length;
		int x = contentX();
		for (int i = 0; i < buttons.length; i++) {
			int width = i == buttons.length - 1 ? contentX() + contentWidth() - x : each;
			ButtonWidget built = buttons[i].dimensions(x, y, width, BUTTON_HEIGHT).build();
			addDrawableChild(built);
			x += each + BUTTON_GAP;
		}
	}

	/** O ponto como está na tela agora: posição original, nome e ícone editados. */
	private Waypoint current() {
		String typed = nameField != null ? nameField.getText() : name;
		return new Waypoint(typed, original.pos(), Registries.ITEM.getId(icon));
	}

	@Override
	public void tick() {
		super.tick();
		// O "apagar" desarma sozinho: um clique esquecido há um minuto não pode valer como
		// confirmação de agora.
		if (deleteArmedAt >= 0 && System.currentTimeMillis() - deleteArmedAt >= CONFIRM_MS) {
			deleteArmedAt = -1;
			clearAndInit();
		}
	}

	// ------------------------------------------------------------------ desenho

	@Override
	protected void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
		for (int i = 0; i < Waypoint.ICONS.size(); i++) {
			int x = iconsX() + (i % iconsPerRow()) * ICON_CELL;
			int y = iconsY() + (i / iconsPerRow()) * ICON_CELL;
			Item item = Waypoint.ICONS.get(i);

			boolean chosen = item == icon;
			boolean hovered = mouseX >= x && mouseX < x + ICON_CELL && mouseY >= y && mouseY < y + ICON_CELL;
			if (chosen) {
				context.fill(x, y, x + ICON_CELL, y + ICON_CELL, COLOR_ACCENT);
				context.fill(x + 1, y + 1, x + ICON_CELL - 1, y + ICON_CELL - 1, 0x40FFFFFF);
			} else if (hovered) {
				context.fill(x, y, x + ICON_CELL, y + ICON_CELL, 0x30000000);
			}
			context.drawItem(new ItemStack(item), x + (ICON_CELL - 16) / 2, y + (ICON_CELL - 16) / 2);
		}

		BlockPos p = original.pos().pos();
		Text coordinates = Text.translatable("screen.pokebook.waypoints.coordinates",
			p.getX(), p.getY(), p.getZ(), WaypointsScreen.dimensionName(original.pos().dimension()));
		context.drawText(textRenderer, coordinates,
			contentX() + (contentWidth() - textRenderer.getWidth(coordinates)) / 2, coordinatesY(), COLOR_MUTED, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			for (int i = 0; i < Waypoint.ICONS.size(); i++) {
				int x = iconsX() + (i % iconsPerRow()) * ICON_CELL;
				int y = iconsY() + (i / iconsPerRow()) * ICON_CELL;
				if (mouseX >= x && mouseX < x + ICON_CELL && mouseY >= y && mouseY < y + ICON_CELL) {
					icon = Waypoint.ICONS.get(i);
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
