package io.github.lxxz.pokebook.integration;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.JoinGroupEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.call.CallService;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * O fio: leva o áudio de quem está numa ligação até o outro lado, e só até ele.
 *
 * <p><b>Esta classe não é carregada por nós.</b> Quem a carrega é o próprio Simple Voice
 * Chat, que lê o entrypoint {@code voicechat} do nosso {@code fabric.mod.json}. Sem o mod
 * instalado, ninguém lê esse entrypoint e a classe nunca é tocada — a mesma fronteira de
 * classe usada para o Cobblemon, só que aqui o mecanismo de entrypoint a garante sozinho,
 * sem precisar de um {@code if} em lugar nenhum.
 *
 * <p><b>É a única classe do mod que menciona o Simple Voice Chat</b>, e a intenção é que
 * continue assim. Todo o telefone — quem liga para quem, tocar, atender, desligar,
 * desistir — está em {@code CallService}, que não sabe que este mod existe. A conversa
 * entre os dois cabe numa pergunta: {@link CallService#peerOf(UUID)} devolve um
 * {@link UUID} ou {@code null}. Nada além de tipos do Java atravessa, e é isso que permite
 * o resto do sistema nascer na {@code main}.
 *
 * <p><b>O mecanismo</b>, que é o caso de uso canônico da API e não uma gambiarra:
 * engancha-se o pacote de microfone, <b>cancela-se</b> e reenvia-se o áudio como
 * <em>static sound packet</em> só para a conexão do destinatário. "Static" quer dizer
 * não-posicional: distância e dimensão deixam de importar, que é exatamente o que uma
 * ligação precisa.
 *
 * <p>⚠️ <b>Consequência de desenho do cancelamento:</b> enquanto se está numa ligação,
 * quem está por perto <b>não ouve</b> este jogador. É diferente de um telefone de verdade,
 * onde quem está na sala ouve metade da conversa. É o preço de suprimir a voz de
 * proximidade no mesmo gesto que desvia o áudio, e combina com o desenho declarado no
 * {@code IDEIAS.md}: a ligação é um canal fechado, e é o que faz o aparelho valer alguma
 * coisa. Reverter isso seria mandar o pacote duas vezes, sem cancelar — dá para fazer, mas
 * é outra decisão.
 */
public class VoicechatIntegration implements VoicechatPlugin {
	/** Id do plugin dentro do Simple Voice Chat. Aparece nos logs dele. */
	public static final String PLUGIN_ID = Pokebook.MOD_ID;

	@Override
	public String getPluginId() {
		return PLUGIN_ID;
	}

	@Override
	public void initialize(VoicechatApi api) {
		Pokebook.LOGGER.info("Simple Voice Chat encontrado; plugin do Pokébook registrado.");
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
		registration.registerEvent(CreateGroupEvent.class, this::onCreateGroup);
		registration.registerEvent(JoinGroupEvent.class, this::onJoinGroup);
	}

	/**
	 * Ninguém cria grupo nesta instalação.
	 *
	 * <p>É a peça que faltava no {@code IDEIAS.md}: sem isto, dava para continuar
	 * conversando à distância pelo grupo do próprio Simple Voice Chat, e o aparelho virava
	 * mais um jeito de fazer o que já dava, em vez de ser o único. Cancelar aqui não é
	 * suposição — conferido no bytecode do jar do mod ({@code ServerGroupManager.addGroup}):
	 * ele consulta {@code PluginManager.onCreateGroup} e retorna sem criar nada se algum
	 * plugin cancelou.
	 */
	private void onCreateGroup(CreateGroupEvent event) {
		event.cancel();
		explainToPlayer(event.getConnection());
	}

	/** Mesma ideia para quem tentar entrar num grupo já existente, criado antes desta versão. */
	private void onJoinGroup(JoinGroupEvent event) {
		event.cancel();
		explainToPlayer(event.getConnection());
	}

	/**
	 * Diz ao jogador por que nada aconteceu.
	 *
	 * <p>Cancelar sem explicar deixa o botão parecendo quebrado: o jogador clica, nada
	 * acontece, e ele não tem como saber que foi de propósito nem que existe outro caminho.
	 *
	 * <p>Com {@code enable_groups=false} no config do servidor isto quase nunca dispara —
	 * o próprio Simple Voice Chat esconde o botão. Este caminho é a rede de segurança para
	 * o servidor onde ninguém configurou.
	 *
	 * <p>⚠️ <b>O envio é agendado na thread do servidor.</b> Este método é chamado pela
	 * thread do Simple Voice Chat, e mandar pacote para um jogador a partir de outra thread
	 * é justamente o tipo de coisa que funciona nos testes e quebra num servidor cheio.
	 */
	private void explainToPlayer(VoicechatConnection connection) {
		if (connection == null || connection.getPlayer() == null) {
			return;
		}
		// getPlayer() da API devolve Object: é o jogador da plataforma, que no Fabric é
		// ServerPlayerEntity. O instanceof também cobre o caso de não ser.
		if (connection.getPlayer().getPlayer() instanceof ServerPlayerEntity player) {
			player.server.execute(() ->
				player.sendMessage(Text.translatable("message.pokebook.groups_disabled"), false));
		}
	}

	/**
	 * Um pacote de microfone acabou de chegar ao servidor.
	 *
	 * <p>Roda para <b>todo</b> pacote de <b>todo</b> jogador que estiver falando, várias
	 * vezes por segundo. Por isso a primeira coisa é a consulta mais barata que existe —
	 * uma busca em mapa — e a imensa maioria das chamadas termina na segunda linha.
	 *
	 * <p>⚠️ <b>Isto não roda na thread do servidor</b>, e sim na de áudio do Simple Voice
	 * Chat. É a razão de o mapa do {@code CallService} ser concorrente; nada aqui pode
	 * tocar no mundo, em entidade ou em inventário.
	 */
	private void onMicrophonePacket(MicrophonePacketEvent event) {
		VoicechatConnection sender = event.getSenderConnection();
		if (sender == null) {
			return;
		}

		UUID peerId = CallService.peerOf(sender.getPlayer().getUuid());
		if (peerId == null) {
			// Não está em ligação: o pacote segue o caminho normal e vira voz de
			// proximidade. Este é o caso comum.
			return;
		}

		// Cancela ANTES de procurar o destinatário, e não depois. Se a conexão dele sumiu
		// entre o nosso estado e este quadro, o certo é a ligação ficar muda — não é voltar
		// a voz de proximidade no meio de uma conversa que os dois acham que é privada.
		event.cancel();

		if (CallService.isMuted(sender.getPlayer().getUuid())) {
			// Mudo não é "sem ligação": a proximidade continua suprimida (o cancel() já
			// aconteceu), só o áudio não atravessa. É a diferença entre desligar e tapar o
			// microfone.
			return;
		}

		VoicechatServerApi api = event.getVoicechat();
		VoicechatConnection peer = api.getConnectionOf(peerId);
		if (peer == null) {
			return;
		}

		api.sendStaticSoundPacketTo(peer, event.getPacket().staticSoundPacketBuilder().build());
	}
}
