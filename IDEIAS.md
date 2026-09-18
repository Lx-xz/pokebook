# Ideias para o futuro

Coisas decididas como desejáveis, mas **não** desenhadas ainda. Nada aqui foi
submetido às rodadas de perguntas — são intenções, não especificações.

Ver `PLANO.md` para o que está em construção e `CLAUDE.md` para as decisões fechadas.

---

## Poképhone

Um segundo item, portátil, irmão do pokébook. O pokébook é a estação — fica na base,
onde você administra missões. O celular é o que você leva no bolso.

A divisão natural seria: **pokébook = missões e administração; poképhone = social e
comunicação.** Mas nada disso está decidido.

## Aba social

O **degrau 1 está feito**: ver o progresso dos outros, como foi previsto — nenhum sistema
novo, só uma consulta e uma tela. Duas limitações conscientes ficaram:

- **Só jogadores conectados.** O progresso de quem está offline mora no arquivo de save
  do jogador, e lê-lo exigiria abrir um arquivo por jogador a cada consulta. Um placar
  histórico é outra funcionalidade, não uma extensão desta.
- **Resumo, não detalhe.** Vai "quantas de quantas", não a lista de missões de cada um.
  Responde a pergunta que a aba existe para responder sem multiplicar o pacote por
  jogadores × missões. Ver a lista de um jogador específico seria um degrau 1.5 natural:
  clicar numa linha e pedir o detalhe só daquele.

**Degrau 2 — mensagens entre jogadores.** Continua caro: precisa de persistência própria,
entrega para quem está offline, histórico, e moderação se houver desconhecidos no
servidor. É um mod inteiro por si só.

## Ligações por voz

A ideia: com o **Simple Voice Chat** instalado para voz de proximidade, o Pokébook
**desabilitaria o sistema de grupos do próprio SVC** — e a única forma de conversar à
distância passaria a ser ligar pelo celular.

Isso é design, não limitação técnica: se dá para falar longe sem o celular, o celular
vira enfeite. Restringir o canal é o que transforma o item em infraestrutura do servidor.

### O que já foi pesquisado e confirmado

- O Simple Voice Chat tem **API pública oficial** para plugins, com Javadoc, repositório
  Maven próprio e suporte confirmado a **Minecraft 1.21.1 / Fabric**.
- No Fabric o registro é por um **entrypoint `voicechat`** no `fabric.mod.json`, separado
  dos entrypoints `main` e `client`.
- **Ligação privada é o caso de uso canônico da API**, não uma gambiarra. O mecanismo:
  enganchar o evento de pacote de microfone, **cancelá-lo** (isso suprime a voz de
  proximidade normal daquele pacote) e reenviar como *static sound packet* apenas para a
  conexão do destinatário. "Static" significa não-posicional — distância e dimensão
  deixam de importar. São por volta de 40 linhas.
- Existe um **plugin de exemplo oficial** do autor do SVC com exatamente esse fluxo, e um
  mod de walkie-talkie (licença MIT, com branch de 1.21.1) que é o parente arquitetural
  mais próximo.
- **A API é bem mais estável que a do Cobblemon**: remoções vêm com `@Deprecated` e
  substituto documentado. A recomendação é compilar contra a versão mais antiga que tenha
  o necessário, para rodar em todas as mais novas.

### Sobre os addons — não valem a pena

- **"Simple Frequency"** é um **datapack**, sem API de desenvolvedor.
- **"Simple Radio"** tem API, mas é **GPLv3** (com implicações de licença para o
  Pokébook) e modela tudo como *frequência* — difusão de muitos-para-muitos, que é a
  forma errada para uma ligação 1-para-1.

Falar direto com a API do Simple Voice Chat é mais simples.

### O que ainda não se sabe

- **Como desabilitar os grupos do SVC.** Há pelo menos dois caminhos plausíveis: a API
  expõe acesso à configuração do servidor, e a conexão de um jogador permite trocar o
  grupo dele. Nenhum dos dois foi verificado, e nem se a imposição seria robusta (o
  jogador poderia simplesmente entrar no grupo de novo).
- **Sinalização de chamada é toda nossa.** Tocar, atender, desligar, identificação de
  quem liga — a API não oferece nada disso. Ela entrega áudio de A para B; o telefone em
  volta é trabalho nosso.

### Considerações práticas

- O SVC precisa estar **no cliente e no servidor**, e o servidor precisa de porta UDP
  configurada. Quem não tiver o mod não fala nem ouve.
- Dependência deve ser **opcional**, com os imports isolados numa classe própria.
  ⚠️ É a **terceira vez** que o mesmo padrão de carregamento de classe aparece neste
  projeto — cliente/servidor, Cobblemon, voice chat. Já é regra da casa: **no Minecraft,
  "código opcional" é sempre fronteira de classe, nunca condicional.**
- Estado de chamada é **por jogador**, igual ao progresso de missão. O bloco continua
  burro, coerente com a arquitetura já decidida.

---

## Redesenho da interface

O autor desenhou um conceito (`docs/pokebook_gui.pixil`, e as capturas em
`docs/screenshots/`) com cara de macOS: barra de título própria com ← à esquerda e ✕ à
direita, botões de **ícone** em vez de texto no menu, linhas de missão como **cartões
arredondados** claros sobre o fundo ciano, e abas "NOVAS / COMPLETAS" separando as
missões por estado. O fundo tem um padrão de silhuetas de Pokémon em marca-d'água.

Os botões de canto já existem. O resto é desenho, e o caminho técnico é o **sistema de
sprites de GUI** da 1.21, não `drawTexture` com coordenadas na mão:

- Cada peça vira um PNG em `assets/pokebook/textures/gui/sprites/<nome>.png`, referida
  por id (`pokebook:<nome>`), e desenhada com `context.drawGuiTexture(id, x, y, w, h)`.
- Um `.png.mcmeta` ao lado marca a peça como **nine-slice**: declara a largura das
  bordas, e o jogo estica só o miolo. É o que permite um cartão arredondado servir a
  qualquer largura e altura sem deformar o canto — exatamente o que os cartões de missão
  e os botões precisam.
- Botão com aparência própria é um `ButtonWidget` cujo `renderWidget` desenha o sprite;
  três sprites (normal, sob o mouse, desabilitado) cobrem os estados.

A vantagem sobre `drawTexture` é não haver número de coordenada espalhado pelo código:
mudar o desenho passa a ser trocar o PNG.

**Decisão em aberto:** as abas "novas/completas" mudam o *modelo* da tela, não só o
desenho — hoje a lista é uma só. Vale decidir se a separação é filtro de cliente ou se
o servidor manda as duas listas.

---

## Outras pendências menores

- **Contorno cheio de linhas** — a escada de quatro caixas desenha quatro wireframes
  sobrepostos. Dá para separar: contorno simples com uma caixa, colisão detalhada com a
  escada. São métodos diferentes, não é preciso escolher.
- **Som ao clicar** — cortado do escopo da v1 de propósito.
- **Advancement de desbloqueio da receita** — a receita funciona, mas não aparece
  sozinha no livro de receitas.
- **Rolagem na lista de missões** — quando passarem de caber na moldura.
- **Missões repetíveis** — exige decidir *quando* reinicia, e essa decisão fica melhor
  depois de ter jogado com o sistema.
