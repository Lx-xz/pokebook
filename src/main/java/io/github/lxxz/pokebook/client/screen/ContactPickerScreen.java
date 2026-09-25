package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.phone.Contact;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * "Para quem?" — escolher um dos seus contatos.
 *
 * <p>Uma tela só para as duas perguntas que têm essa forma: com quem começar uma conversa, e
 * para quem mandar uma foto. Quem abre diz o título, para onde voltar e o que fazer com a
 * escolha.
 *
 * <p>Mostra só os contatos, e não todo mundo online: mensagem e foto só chegam a quem salvou
 * você, e quem não está nos seus contatos quase certamente também não te salvou. Oferecer
 * seria oferecer algo que o servidor recusaria.
 */
public class ContactPickerScreen extends ScrollListScreen<Contact> {
	private static final int FACE = 16;

	private final Supplier<PokebookScreenBase> parent;
	private final BiConsumer<ContactPickerScreen, Contact> onPick;

	/**
	 * @param onPick recebe a própria tela, para poder navegar a partir dela
	 */
	public ContactPickerScreen(PokebookSession session, Text title, Supplier<PokebookScreenBase> parent,
			BiConsumer<ContactPickerScreen, Contact> onPick) {
		super(title, session);
		this.parent = parent;
		this.onPick = onPick;
	}

	@Override
	protected PokebookScreenBase parentScreen() {
		return parent.get();
	}

	/** Deixa quem escolheu navegar para a próxima tela sem perder a sessão com o bloco. */
	public void go(PokebookScreenBase next) {
		navigateTo(next);
	}

	@Override
	protected Text emptyText() {
		return Text.translatable("screen.pokebook.picker.empty");
	}

	@Override
	protected List<Contact> rows() {
		List<Contact> contacts = new ArrayList<>(ClientPhone.data().contacts());
		contacts.sort(Comparator.comparing(Contact::name, String.CASE_INSENSITIVE_ORDER));
		return contacts;
	}

	@Override
	protected void renderRow(DrawContext context, Contact contact, int x, int y, int width, boolean hovered) {
		boolean online = PlayerFaces.isOnline(contact.uuid());
		PlayerFaces.draw(context, PlayerFaces.skin(contact.uuid()), x, y + (ROW_HEIGHT - FACE) / 2, FACE, online);
		context.drawText(textRenderer, textRenderer.trimToWidth(contact.name(), width - FACE - 6), x + FACE + 4,
			y + (ROW_HEIGHT - textRenderer.fontHeight) / 2, hovered ? COLOR_ACCENT : COLOR_TEXT, false);
	}

	@Override
	protected void onRowClicked(Contact contact, int index, double localX) {
		onPick.accept(this, contact);
	}
}
