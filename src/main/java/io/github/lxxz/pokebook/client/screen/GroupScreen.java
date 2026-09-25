package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.network.GroupInvitePayload;
import io.github.lxxz.pokebook.network.GroupMember;
import io.github.lxxz.pokebook.network.LeaveGroupPayload;
import io.github.lxxz.pokebook.network.RequestGroupPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * O grupo das missões cooperativas: quem está nele, convidar, sair.
 *
 * <p>Nos dois aparelhos, como os contatos: o grupo é das missões, e missão se vê nos dois.
 *
 * <p>Convidar abre o seletor de contatos. Só vai quem está online e já salvou você — o
 * servidor confere e responde acima da hotbar se não der. Sair pede dois cliques: o contador
 * fica com o grupo, e voltar exige convite de novo.
 */
public class GroupScreen extends ScrollListScreen<GroupMember> {
	private static final int FACE = 16;
	private static final long CONFIRM_MS = 3000;

	private List<GroupMember> members = List.of();
	private boolean loaded;
	private long leaveArmedAt = -1;

	public GroupScreen(PokebookSession session) {
		super(Text.translatable("screen.pokebook.group"), session);
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
		if (!loaded) {
			ClientPlayNetworking.send(new RequestGroupPayload());
		}
		boolean armed = leaveArmedAt >= 0;
		List<ButtonWidget> row = addFooterRow(0,
			ButtonWidget.builder(Text.translatable("screen.pokebook.group.invite"), button ->
				navigateTo(new ContactPickerScreen(session, Text.translatable("screen.pokebook.group.invite"),
					() -> new GroupScreen(session),
					(picker, contact) -> {
						ClientPlayNetworking.send(new GroupInvitePayload(contact.uuid()));
						picker.go(new GroupScreen(session));
					}))),
			ButtonWidget.builder(Text.translatable(armed ? "screen.pokebook.group.leave_confirm" : "screen.pokebook.group.leave"),
				button -> leave()));
		row.get(1).active = !members.isEmpty();
	}

	private void leave() {
		// Dois cliques: sair não tem volta sem um convite novo.
		if (leaveArmedAt < 0 || System.currentTimeMillis() - leaveArmedAt >= CONFIRM_MS) {
			leaveArmedAt = System.currentTimeMillis();
			clearAndInit();
			return;
		}
		leaveArmedAt = -1;
		ClientPlayNetworking.send(new LeaveGroupPayload());
	}

	@Override
	public void tick() {
		super.tick();
		if (leaveArmedAt >= 0 && System.currentTimeMillis() - leaveArmedAt >= CONFIRM_MS) {
			leaveArmedAt = -1;
			clearAndInit();
		}
	}

	/** Chamado quando o grupo chega do servidor — pedido ou porque mudou. */
	public void update(List<GroupMember> updated) {
		members = updated;
		loaded = true;
		clearAndInit();
	}

	@Override
	protected Text emptyText() {
		return Text.translatable(loaded ? "screen.pokebook.group.empty" : "screen.pokebook.loading");
	}

	@Override
	protected List<GroupMember> rows() {
		return members;
	}

	@Override
	protected void renderRow(DrawContext context, GroupMember member, int x, int y, int width, boolean hovered) {
		PlayerFaces.draw(context, PlayerFaces.skin(member.uuid()), x, y + (ROW_HEIGHT - FACE) / 2, FACE, member.online());
		context.drawText(textRenderer, textRenderer.trimToWidth(member.name(), width - FACE - 4),
			x + FACE + 4, y + (ROW_HEIGHT - textRenderer.fontHeight) / 2,
			member.online() ? COLOR_TEXT : COLOR_MUTED, false);
	}

	@Override
	protected void onRowClicked(GroupMember member, int index, double localX) {
	}
}
