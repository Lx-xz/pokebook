package io.github.lxxz.pokebook.integration;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import io.github.lxxz.pokebook.Pokebook;

/**
 * Ponte com o Simple Voice Chat. Por enquanto só prova que o gancho existe.
 *
 * <p><b>Esta classe não é carregada por nós.</b> Quem a carrega é o próprio Simple Voice
 * Chat, que lê o entrypoint {@code voicechat} do nosso {@code fabric.mod.json}. Sem o mod
 * instalado, ninguém lê esse entrypoint e a classe nunca é tocada — a mesma fronteira de
 * classe usada para o Cobblemon, só que aqui o próprio mecanismo de entrypoint a garante,
 * sem precisar de um {@code if} em lugar nenhum.
 *
 * <p>É por isso que o Simple Voice Chat entra como dependência <b>opcional</b>: o pokébook
 * segue funcionando sem ele, apenas sem chamadas.
 *
 * <p><b>O caminho para a ligação privada</b>, quando chegarmos lá, já está mapeado no
 * {@code IDEIAS.md}: enganchar {@link MicrophonePacketEvent}, <b>cancelá-lo</b> — o que
 * suprime a voz de proximidade daquele pacote — e reenviar o áudio como <em>static sound
 * packet</em> só para a conexão do destinatário. "Static" quer dizer não-posicional:
 * distância e dimensão deixam de importar. São por volta de 40 linhas.
 *
 * <p>O que a API <b>não</b> dá é o telefone em volta: tocar, atender, desligar e
 * identificar quem liga são trabalho nosso.
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
		// Ainda sem evento nenhum. Quando a ligação entrar, é aqui que o
		// MicrophonePacketEvent é assinado.
	}
}
