package io.github.lxxz.pokebook.client.screen;

import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.notify.NotificationKind;
import net.minecraft.util.Identifier;

/**
 * Qual desenho cada app usa.
 *
 * <p>⚠️ <b>Provisório, e de propósito.</b> Existem três desenhos — telefone, balão e
 * pergaminho — e onze apps. Até cada um ter arte própria, os apps <b>reusam</b> o desenho
 * mais parecido, agrupados por família:
 *
 * <ul>
 *   <li><b>telefone</b> — falar com pessoas: ligações, contatos;</li>
 *   <li><b>balão</b> — avisos e texto: social, notificações, notas;</li>
 *   <li><b>pergaminho</b> — o resto, que é "consultar alguma coisa": missões, pontos,
 *       radar, relógio, fotos, ranking, ajustes.</li>
 * </ul>
 *
 * <p>O rótulo embaixo do ícone é o que diferencia os apps enquanto isso. Para dar arte
 * própria a um app, basta desenhar {@code textures/gui/sprites/icone_<nome>.png} em
 * branco, 32×32 (ver o {@code CLAUDE.md} sobre tingimento) e trocar a linha dele aqui por
 * {@code icon("icone_<nome>")}. Nenhum outro arquivo precisa saber.
 */
public final class AppIcons {
	private static final Identifier PHONE = icon("icone_ligacoes");
	private static final Identifier BUBBLE = icon("icone_mensagens");
	private static final Identifier SCROLL = icon("icone_missoes");

	// Com arte própria
	public static final Identifier MISSIONS = SCROLL;
	public static final Identifier CALLS = PHONE;
	public static final Identifier SOCIAL = BUBBLE;

	// Reusando, até ter arte
	public static final Identifier CONTACTS = PHONE;
	public static final Identifier NOTIFICATIONS = BUBBLE;
	public static final Identifier NOTES = BUBBLE;
	public static final Identifier WAYPOINTS = SCROLL;
	public static final Identifier RADAR = SCROLL;
	public static final Identifier CLOCK = SCROLL;
	public static final Identifier PHOTOS = SCROLL;
	public static final Identifier RANKING = SCROLL;
	public static final Identifier SETTINGS = SCROLL;
	public static final Identifier PC = SCROLL;
	public static final Identifier MESSAGES = BUBBLE;

	private AppIcons() {
	}

	/** Um desenho pelo nome do arquivo em {@code textures/gui/sprites/}. */
	public static Identifier icon(String name) {
		return Identifier.of(Pokebook.MOD_ID, name);
	}

	/** O ícone de uma notificação é o do app de onde ela vem. */
	public static Identifier forNotification(NotificationKind kind) {
		return switch (kind) {
			case MISSION -> MISSIONS;
			case CALL -> CALLS;
			case CONTACT -> CONTACTS;
			case LOCATION -> WAYPOINTS;
			case ALARM -> CLOCK;
			case PHOTO -> PHOTOS;
			case MESSAGE -> MESSAGES;
		};
	}
}
