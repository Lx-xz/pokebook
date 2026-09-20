package io.github.lxxz.pokebook.call;

import io.github.lxxz.pokebook.network.CallStatePayload;
import io.github.lxxz.pokebook.registry.ModItems;
import io.github.lxxz.pokebook.sound.PokebookSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Quem está falando com quem — o telefone em volta do áudio.
 *
 * <p><b>Esta classe não sabe que o Simple Voice Chat existe.</b> Tocar, atender, recusar,
 * desligar e desistir por falta de resposta são coisas nossas: a API do SVC entrega áudio
 * de A para B e nada mais. A separação não é elegância — é a mesma fronteira usada para o
 * Cobblemon, e é o que permite este arquivo nascer na {@code main} enquanto só
 * {@code integration/VoicechatIntegration} fica preso à branch da dependência.
 *
 * <p>O único ponto de contato é {@link #peerOf(UUID)}: o roteador de áudio pergunta "para
 * quem mando isto?" e recebe um {@link UUID} ou {@code null}. Nada além de tipos do Java
 * atravessa.
 *
 * <p><b>Estado por jogador, como o progresso de missão.</b> O aparelho continua burro —
 * ligar não depende de estar segurando o poképhone, e uma ligação sobrevive a guardar o
 * item no baú. O que ela não sobrevive é desconectar, e isso é de propósito: a ligação
 * vive em memória, como os espectadores da tela. Um servidor que cai derruba as ligações,
 * igual a um servidor que cai derruba as chamadas telefônicas de verdade.
 *
 * <p>⚠️ <b>O mapa é concorrente por necessidade, não por precaução.</b> Todo o resto desta
 * classe roda na thread do servidor, mas {@link #peerOf} é chamado pela thread de áudio do
 * Simple Voice Chat, que é outra. Um {@code HashMap} comum lido de duas threads pode
 * devolver lixo ou entrar em laço infinito, e não apenas um valor velho.
 */
public final class CallService {
	/** Id do Simple Voice Chat no Fabric. É uma string: mencioná-lo não é depender dele. */
	private static final String VOICECHAT_MOD_ID = "voicechat";

	/** Quanto o aparelho toca antes de desistir. Meio minuto. */
	private static final int RING_TIMEOUT_TICKS = 30 * 20;

	/**
	 * De quanto em quanto tempo o aviso acima da hotbar é repetido.
	 *
	 * <p>Precisa existir porque a mensagem de sobreposição do vanilla <b>desaparece
	 * sozinha</b> depois de alguns segundos. Sem repetir, o único indicador de que se está
	 * numa ligação sumiria enquanto a ligação continua.
	 */
	private static final int NOTICE_INTERVAL_TICKS = 20;

	/**
	 * Uma ligação, do ponto de vista de fora.
	 *
	 * <p>Quem ligou e quem foi chamado são guardados separados, e não como "dois
	 * participantes", porque a assimetria dura a ligação inteira: só quem foi chamado pode
	 * atender, e as mensagens de fim dizem coisas diferentes para cada lado.
	 */
	private static final class Call {
		final UUID caller;
		final UUID callee;
		boolean answered;
		int ticks;
		boolean callerMuted;
		boolean calleeMuted;

		Call(UUID caller, UUID callee) {
			this.caller = caller;
			this.callee = callee;
		}

		UUID other(UUID player) {
			return player.equals(caller) ? callee : caller;
		}

		boolean isMuted(UUID player) {
			return player.equals(caller) ? callerMuted : calleeMuted;
		}

		void setMuted(UUID player, boolean muted) {
			if (player.equals(caller)) {
				callerMuted = muted;
			} else {
				calleeMuted = muted;
			}
		}
	}

	/** Os dois participantes apontam para a <b>mesma</b> instância, e é assim que se acham. */
	private static final Map<UUID, Call> BY_PLAYER = new ConcurrentHashMap<>();

	private CallService() {
	}

	/**
	 * Dá para ligar nesta instalação?
	 *
	 * <p>Sem o Simple Voice Chat a ligação seria um telefone mudo: a sinalização inteira
	 * funcionaria e ninguém ouviria nada. É pior do que não ter o botão, então o botão não
	 * existe — o cliente esconde, e o servidor recusa por garantia, que é onde a regra
	 * vale de verdade.
	 */
	public static boolean available() {
		return FabricLoader.getInstance().isModLoaded(VOICECHAT_MOD_ID);
	}

	/**
	 * Para quem o áudio deste jogador deve ir agora, ou {@code null}.
	 *
	 * <p><b>Só responde com a ligação atendida.</b> Enquanto toca, os dois lados já têm uma
	 * ligação registrada, e devolver o par aqui faria quem chamou ser ouvido antes de
	 * atenderem — um grampo, não um telefone.
	 *
	 * <p>Chamado pela thread de áudio do Simple Voice Chat. Ver a nota sobre concorrência
	 * no topo da classe.
	 */
	public static UUID peerOf(UUID player) {
		Call call = BY_PLAYER.get(player);
		return call != null && call.answered ? call.other(player) : null;
	}

	/**
	 * Este jogador tapou o próprio microfone na ligação atual?
	 *
	 * <p>Chamado pela thread de áudio do Simple Voice Chat, igual a {@link #peerOf}, e pela
	 * mesma razão é uma leitura só no mapa concorrente — nada de tocar no mundo aqui.
	 */
	public static boolean isMuted(UUID player) {
		Call call = BY_PLAYER.get(player);
		return call != null && call.isMuted(player);
	}

	/**
	 * Muta ou desmuta quem pediu, na ligação em que estiver.
	 *
	 * <p>Sem ligação não faz nada — não há o que mutar, e o cliente não deveria nem
	 * oferecer o botão fora de uma chamada ativa.
	 */
	public static void toggleMute(ServerPlayerEntity player) {
		Call call = BY_PLAYER.get(player.getUuid());
		if (call == null) {
			return;
		}
		call.setMuted(player.getUuid(), !call.isMuted(player.getUuid()));
		sendState(player);
	}

	/** Em que pé está a ligação deste jogador. */
	public static CallState stateOf(UUID player) {
		Call call = BY_PLAYER.get(player);
		if (call == null) {
			return CallState.IDLE;
		}
		if (call.answered) {
			return CallState.ACTIVE;
		}
		return player.equals(call.caller) ? CallState.DIALING : CallState.RINGING;
	}

	/**
	 * Liga para alguém, pelo nome.
	 *
	 * <p>Pelo nome e não pelo {@link UUID} porque é o que o cliente tem de graça: a lista
	 * de jogadores conectados que a tecla Tab mostra já está na máquina dele. Uma lista
	 * própria vinda do servidor seria um pacote a mais para repetir informação que o jogo
	 * já sincroniza.
	 *
	 * <p>Toda recusa vira mensagem para quem tentou. O pacote vem do cliente e não é
	 * confiável: nada aqui confia em o botão ter sido desenhado.
	 */
	public static void dial(ServerPlayerEntity caller, String targetName) {
		if (!available()) {
			caller.sendMessage(Text.translatable("message.pokebook.call.unavailable"), false);
			return;
		}

		ServerPlayerEntity target = caller.server.getPlayerManager().getPlayer(targetName);
		if (target == null) {
			caller.sendMessage(Text.translatable("message.pokebook.call.offline", targetName), false);
			return;
		}
		if (target == caller) {
			caller.sendMessage(Text.translatable("message.pokebook.call.self"), false);
			return;
		}
		if (BY_PLAYER.containsKey(caller.getUuid())) {
			// Já está numa ligação. Desligar primeiro é decisão do jogador, não nossa.
			caller.sendMessage(Text.translatable("message.pokebook.call.already"), false);
			return;
		}
		if (BY_PLAYER.containsKey(target.getUuid())) {
			caller.sendMessage(Text.translatable("message.pokebook.call.busy", displayName(target)), false);
			return;
		}

		Call call = new Call(caller.getUuid(), target.getUuid());
		BY_PLAYER.put(caller.getUuid(), call);
		BY_PLAYER.put(target.getUuid(), call);

		sendState(caller);
		sendState(target);
		notify(caller.server, call);
	}

	/**
	 * Atende.
	 *
	 * <p>Só vale para quem <b>foi chamado</b> e ainda não atendeu. Mandar este pacote em
	 * qualquer outra situação não faz nada — o cliente não é a autoridade sobre quem pode
	 * atender o quê.
	 */
	public static void answer(ServerPlayerEntity player) {
		Call call = BY_PLAYER.get(player.getUuid());
		if (call == null || call.answered || !player.getUuid().equals(call.callee)) {
			return;
		}

		call.answered = true;
		// Zerado porque o contador passa a medir outra coisa: era paciência, vira duração.
		call.ticks = 0;

		MinecraftServer server = player.server;
		forEachOnline(server, call, participant -> {
			sendState(participant);
			PokebookSounds.playTo(participant, PokebookSounds.SCREEN_ON, 0.5f, 1.4f);
		});
	}

	/**
	 * Desliga, recusa ou desiste — é tudo o mesmo gesto.
	 *
	 * <p>Do lado do cliente há um botão só, e o que ele quer dizer depende do estado:
	 * recusar enquanto toca, desistir enquanto chama, desligar em ligação. Separar em três
	 * pacotes daria três nomes para a mesma frase ("me tire desta ligação") e obrigaria o
	 * servidor a conferir se o cliente escolheu o certo.
	 *
	 * <p>Quem fica do outro lado é quem precisa saber <em>qual</em> dos três foi, e é só
	 * isso que a mensagem distingue.
	 */
	public static void hangUp(ServerPlayerEntity player) {
		Call call = BY_PLAYER.get(player.getUuid());
		if (call == null) {
			return;
		}

		UUID otherUuid = call.other(player.getUuid());
		ServerPlayerEntity other = player.server.getPlayerManager().getPlayer(otherUuid);

		Text noticeToOther;
		if (call.answered) {
			noticeToOther = Text.translatable("message.pokebook.call.hung_up", displayName(player));
		} else if (player.getUuid().equals(call.callee)) {
			noticeToOther = Text.translatable("message.pokebook.call.declined", displayName(player));
		} else {
			noticeToOther = Text.translatable("message.pokebook.call.gave_up");
		}

		end(call);
		sendState(player);
		if (other != null) {
			other.sendMessage(noticeToOther, false);
			sendState(other);
			PokebookSounds.playTo(other, PokebookSounds.SCREEN_OFF, 0.5f, 1.0f);
		}
		PokebookSounds.playTo(player, PokebookSounds.SCREEN_OFF, 0.5f, 1.0f);
	}

	/**
	 * Quem desconecta não desliga o telefone.
	 *
	 * <p>Sem isto, o outro lado ficaria em ligação com um fantasma: o estado dele não
	 * mudaria, o aviso continuaria na tela e o áudio seria mandado para uma conexão que
	 * não existe mais.
	 */
	public static void disconnect(ServerPlayerEntity player) {
		Call call = BY_PLAYER.get(player.getUuid());
		if (call == null) {
			return;
		}

		ServerPlayerEntity other = player.server.getPlayerManager().getPlayer(call.other(player.getUuid()));
		end(call);
		if (other != null) {
			other.sendMessage(Text.translatable("message.pokebook.call.hung_up", displayName(player)), false);
			sendState(other);
			PokebookSounds.playTo(other, PokebookSounds.SCREEN_OFF, 0.5f, 1.0f);
		}
	}

	/**
	 * O tique do telefone: repete os avisos e desiste de quem não atende.
	 *
	 * <p>Roda a cada tick do servidor, mas quase sempre não faz nada — o mapa está vazio na
	 * imensa maioria dos ticks, e o laço sobre ele custa isso.
	 */
	public static void tick(MinecraftServer server) {
		if (BY_PLAYER.isEmpty()) {
			return;
		}

		// Cada ligação aparece duas vezes no mapa, uma por participante. Percorrer só as
		// entradas de quem ligou a visita uma vez. A cópia é porque desistir mexe no mapa,
		// e alterá-lo durante a iteração é o caminho para ConcurrentModificationException.
		List<Call> calls = new ArrayList<>();
		for (Map.Entry<UUID, Call> entry : BY_PLAYER.entrySet()) {
			if (entry.getKey().equals(entry.getValue().caller)) {
				calls.add(entry.getValue());
			}
		}

		for (Call call : calls) {
			call.ticks++;

			if (!call.answered && call.ticks >= RING_TIMEOUT_TICKS) {
				giveUp(server, call);
				continue;
			}

			if (call.ticks % NOTICE_INTERVAL_TICKS == 0) {
				notify(server, call);
			}
		}
	}

	/** Manda para este jogador o estado atual da ligação dele. */
	public static void sendState(ServerPlayerEntity player) {
		Call call = BY_PLAYER.get(player.getUuid());
		String peer = "";
		boolean muted = false;
		if (call != null) {
			ServerPlayerEntity other = player.server.getPlayerManager().getPlayer(call.other(player.getUuid()));
			peer = other == null ? "" : displayName(other);
			muted = call.isMuted(player.getUuid());
		}
		ServerPlayNetworking.send(player, new CallStatePayload(stateOf(player.getUuid()), peer, muted));
	}

	/**
	 * O aviso acima da hotbar, repetido enquanto a ligação durar.
	 *
	 * <p>É de propósito que a notificação viva aí e não numa camada de interface nossa: o
	 * aparelho toca <b>no bolso</b>, e quem é chamado precisa saber disso sem estar com
	 * nenhuma tela aberta. A sobreposição do vanilla já é exatamente esse lugar, e sai sem
	 * um mixin nem um gancho de desenho.
	 */
	private static void notify(MinecraftServer server, Call call) {
		ServerPlayerEntity caller = server.getPlayerManager().getPlayer(call.caller);
		ServerPlayerEntity callee = server.getPlayerManager().getPlayer(call.callee);

		if (call.answered) {
			if (caller != null && callee != null) {
				caller.sendMessage(Text.translatable("message.pokebook.call.active", displayName(callee)), true);
				callee.sendMessage(Text.translatable("message.pokebook.call.active", displayName(caller)), true);
			}
			return;
		}

		if (caller != null && callee != null) {
			caller.sendMessage(Text.translatable("message.pokebook.call.dialing", displayName(callee)), true);
			if (hasPhone(callee)) {
				callee.sendMessage(Text.translatable("message.pokebook.call.ringing", displayName(caller)), true);
				// Só para quem é chamado: o toque é o que chama a atenção de quem não está
				// esperando nada. Quem ligou já sabe que ligou.
				PokebookSounds.playTo(callee, PokebookSounds.RING, 0.7f, 1.6f);
			}
			// Sem aparelho, quem é chamado não tem como saber nem como atender — avisar
			// só confundiria alguém que nunca ouviu falar do mod. A ligação continua
			// tocando e desiste sozinha depois de meio minuto, igual a ninguém atender.
		}
	}

	/**
	 * Tem como este jogador perceber que está sendo chamado?
	 *
	 * <p>O aviso acima da hotbar e o toque só fazem sentido para quem tem <b>um poképhone
	 * em algum lugar do inventário</b> — sem ele não há como abrir a tela e atender. A
	 * pokébook (o bloco) não entra aqui: o aviso soa longe de qualquer estação, e andar até
	 * uma para atender não é o que o desenho da ligação pede.
	 *
	 * <p>Continuar aparecendo na lista de quem chamar, mesmo sem aparelho, é decisão de
	 * propósito por ora — tirar quem não tem celular da lista é outra mudança, registrada
	 * à parte.
	 */
	private static boolean hasPhone(ServerPlayerEntity player) {
		return player.getInventory().contains(stack -> stack.isOf(ModItems.POKEPHONE));
	}

	/** Tocou meio minuto e ninguém atendeu. */
	private static void giveUp(MinecraftServer server, Call call) {
		ServerPlayerEntity caller = server.getPlayerManager().getPlayer(call.caller);
		ServerPlayerEntity callee = server.getPlayerManager().getPlayer(call.callee);

		end(call);

		if (caller != null) {
			caller.sendMessage(Text.translatable("message.pokebook.call.no_answer",
				callee == null ? "" : displayName(callee)), false);
			sendState(caller);
			PokebookSounds.playTo(caller, PokebookSounds.SCREEN_OFF, 0.5f, 1.0f);
		}
		if (callee != null) {
			sendState(callee);
		}
	}

	private static void end(Call call) {
		BY_PLAYER.remove(call.caller, call);
		BY_PLAYER.remove(call.callee, call);
	}

	private static void forEachOnline(MinecraftServer server, Call call,
			Consumer<ServerPlayerEntity> action) {
		ServerPlayerEntity caller = server.getPlayerManager().getPlayer(call.caller);
		ServerPlayerEntity callee = server.getPlayerManager().getPlayer(call.callee);
		if (caller != null) {
			action.accept(caller);
		}
		if (callee != null) {
			action.accept(callee);
		}
	}

	/** O apelido, que é o que a lista de jogadores do cliente também usa para identificar. */
	private static String displayName(ServerPlayerEntity player) {
		return player.getGameProfile().getName();
	}
}
