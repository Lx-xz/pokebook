package io.github.lxxz.pokebook.client.notify;

import io.github.lxxz.pokebook.client.ClientSounds;
import io.github.lxxz.pokebook.client.phone.ClientPhone;
import io.github.lxxz.pokebook.notify.NotificationKind;
import io.github.lxxz.pokebook.sound.PokebookSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A central de notificações, do lado do cliente: mostra o aviso e guarda o histórico.
 *
 * <p>Tudo chega por {@link #push}: o que vem do servidor (missão, ligação perdida, contato,
 * localização) e o que nasce aqui mesmo (alarme, foto salva). É isso que faz ser <em>uma</em>
 * central — o jogador aprende um lugar só.
 *
 * <p><b>O "não perturbe" é decidido aqui</b>, e decide só se o aviso <em>aparece</em>: com o
 * modo ligado ele vai direto para o histórico, sem deslizar na tela nem tocar. Nada se
 * perde — uma ligação perdida durante o modo avião é justamente o que se quer achar depois.
 * O alarme é a exceção, ver {@link NotificationKind#bypassesDoNotDisturb()}.
 *
 * <p>O histórico vive só em memória. É para "o que aconteceu enquanto eu não olhava", não
 * um arquivo — e zera ao sair do mundo, porque os avisos de um servidor não dizem nada no
 * outro.
 */
public final class ClientNotifications {
	/** Quantos avisos o histórico guarda. Os mais velhos saem para os novos entrarem. */
	private static final int HISTORY_SIZE = 30;

	public record Entry(NotificationKind kind, Text title, Text body, long timeMillis) {
	}

	private static final Deque<Entry> HISTORY = new ArrayDeque<>();
	private static int unread;

	private ClientNotifications() {
	}

	public static void push(NotificationKind kind, Text title, Text body) {
		HISTORY.addFirst(new Entry(kind, title, body, System.currentTimeMillis()));
		while (HISTORY.size() > HISTORY_SIZE) {
			HISTORY.removeLast();
		}
		unread++;

		boolean silenced = ClientPhone.data().settings().doNotDisturb() && !kind.bypassesDoNotDisturb();
		if (silenced) {
			return;
		}

		MinecraftClient.getInstance().getToastManager().add(new PhoneToast(kind, title, body));
		ClientSounds.play(kind == NotificationKind.ALARM ? PokebookSounds.RING : PokebookSounds.NOTIFY,
			0.8f, kind == NotificationKind.ALARM ? 1.6f : 1.2f);
	}

	/** Do mais novo para o mais velho. */
	public static List<Entry> history() {
		return List.copyOf(HISTORY);
	}

	/** Quantos chegaram desde a última vez que o jogador abriu a lista. */
	public static int unread() {
		return unread;
	}

	public static void markRead() {
		unread = 0;
	}

	public static void clear() {
		HISTORY.clear();
		unread = 0;
	}
}
