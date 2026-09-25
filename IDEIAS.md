# Ideias para o futuro

Coisas decididas como desejáveis, mas **não** desenhadas ainda. Nada aqui foi
submetido às rodadas de perguntas — são intenções, não especificações.

Ver `PLANO.md` para o que está em construção e `CLAUDE.md` para as decisões fechadas.

---

## Poképhone

**Existe, em versão de teste** — item registrado, abre a mesma interface em pé, usando o
modelo do pokébook até ter o seu. Receita: um pokébook, ouro, redstone e um fragmento de
ametista.

⚠️ **A receita consome o pokébook.** É a forma mais clara de fazer o celular custar mais,
e cria uma progressão — notebook primeiro, celular depois. Mas como o resgate só acontece
no pokébook, quem fizer o celular vai precisar craftar outro. Isso é coerente ou
irritante? Decisão em aberto.

### O que impede um de substituir o outro

O risco declarado: se o celular faz tudo e cabe no bolso, o pokébook vira decoração — o
que o projeto não quer ser. Uma regra já está no código:

- **Resgate só no pokébook.** Conferido no servidor, não no cliente: esconder o botão é
  aparência, e o pacote de resgate continua sendo um pacote que qualquer cliente pode
  mandar. A autorização usa o conjunto de espectadores que já existia para a animação da
  tela.

Ideias para aprofundar a divisão, nenhuma decidida:

- **Celular notifica, pokébook administra.** O celular avisa "missão concluída" e mostra
  progresso; configurar, resgatar e ver o social exigem sentar na estação.
- **Celular só lê, pokébook escreve.** Vale para tudo que vier depois: aceitar, descartar
  ou trocar missões seriam ações de estação.
- **Energia.** O celular gasta, o pokébook não — um está no bolso, o outro na tomada. A
  ideia do autor é uma **configuração booleana**, desligada por padrão, com recarga por
  fonte disponível: algo do vanilla, algo do Cobblemon se instalado, energia se houver mod
  de energia. Cada fonte é uma classe isolada, pelo mesmo padrão de fronteira já usado
  três vezes aqui.
- **Alcance.** O celular funciona em qualquer lugar; o pokébook poderia dar algo que o
  celular não dá — mais missões ativas, ou recompensa melhor por resgatar na estação.

### Animação de olhar para o celular

O autor quer que o personagem olhe para o poképhone enquanto o usa. É possível, mas é
frente nova:

- O Minecraft tem um conjunto **fechado** de animações de uso de item — comer, beber,
  bloquear, arco, lança, besta, luneta, corneta, pincel. Não se declara uma nova por
  JSON. A luneta é a mais próxima, mas exige **segurar** o botão, e o celular abre a tela
  num clique e solta.
- Fazer o boneco olhar para baixo exigiria um estado "usando o celular" **sincronizado**
  para os outros jogadores, e um **mixin** no modelo do jogador para dobrar cabeça e
  braço.

O projeto já tem um mixin (o que esconde a mão), então a ferramenta existe. O que falta é
o estado sincronizado, que é o trabalho de verdade.

### Receita alternativa com itens do Cobblemon

Dois caminhos para o mesmo item é normal no Minecraft: são duas receitas com ids
diferentes e o mesmo resultado. E a divisão de branches cai bem aqui — a receita que usa
itens do Cobblemon **nasce na branch `cobblemon`**, porque uma receita que cita um item
inexistente não carrega.

## Missões diárias sorteadas

Desenho do autor, ainda não implementado. É a mudança que resolve o problema real de
hoje: missão é única, e depois de uma tarde o pokébook fica vazio.

**A forma:** 3 a 5 missões por dia, sorteadas de um conjunto, com **faixas de
dificuldade** — fácil dá menos, média dá mais, difícil dá mais ainda. A distribuição pode
ser fixa (duas fáceis, duas médias, uma difícil) ou sorteada **com mínimos**, para não
sair um dia inteiro de fáceis nem um inteiro de difíceis.

**A ideia que muda o modelo: missão vira TIPO de missão.** Não faz sentido ter uma missão
"derrotar 2 Pidgey" e outra "derrotar 2 Rattata" escritas à mão. Faz sentido ter um tipo
"derrotar fácil", que sorteia **um Pokémon fácil qualquer** e uma quantidade. Outro tipo:
"derrotar Pokémon de nível acima do seu melhor" — ou acima de um nível sorteado.

> ⚠️ **Isto é uma mudança de modelo, não uma funcionalidade a mais.** Hoje uma `Mission` é
> uma definição estática vinda de datapack, e o id dela é o caminho do arquivo. Com
> sorteio, o arquivo de datapack vira um **molde**, e o que o jogador tem são **instâncias**
> — "derrotar 4 Rattata, hoje". Isso exige:
>
> - id de instância, distinto do id do molde, para o progresso saber a que se refere;
> - guardar as instâncias do dia **por jogador**, junto do progresso;
> - guardar **quando** o dia virou, para saber quando sortear de novo;
> - decidir o fuso — o do servidor é o único que não depende de cliente.
>
> O `MissionProgress` de hoje mapeia id → contagem, e isso continua servindo; o que entra
> é o conjunto de instâncias ativas ao lado dele.

**Consequência na interface:** com sorteio diário, "concluídas" vira uma lista que só
cresce. O autor propôs mostrar **as últimas 10** e, ao lado, **um número** com o total
concluído. Isso resolve tanto o tamanho do pacote quanto a rolagem infinita.

**Ainda por decidir:** o que define "fácil", "médio" e "difícil"? Pode ser etiqueta no
molde, escrita à mão, ou derivada de dado do Cobblemon — taxa de captura, estágio de
evolução, se é lendário. Etiqueta à mão é mais simples e não quebra quando o Cobblemon
muda.

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

### O que já está escrito

A ligação em si **existe** na branch `voicechat`, em duas metades: o telefone
(`CallService` e a tela de ligações, livres do SVC) e o fio (`VoicechatIntegration`, que
cancela o pacote de microfone e o reenvia ao destinatário). Ver o `PLANO.md` para o
desenho e para o que falta conferir em jogo.

O que era "sinalização é toda nossa" virou código: tocar, atender, recusar, desligar,
desistir depois de meio minuto e identificar quem liga. O aviso de chamada vive **acima da
hotbar**, onde alcança quem não está com tela nenhuma aberta.

### Grupos do SVC — resolvido, em duas camadas

**A forma certa é uma opção nativa do próprio Simple Voice Chat**, e ela só apareceu
depois que o autor reclamou do botão que não fazia nada. No config do servidor
(`config/voicechat/voicechat-server.properties`):

```properties
# If group chats are allowed
enable_groups=false
```

Três classes do mod leem essa opção, e juntas resolvem o problema inteiro sem uma linha
nossa: `ServerGroupManager` recusa no servidor, `VoiceChatScreen` **esconde o botão** no
cliente, e `SecretPacket` sincroniza a opção do servidor para o cliente ao conectar. É o
caminho suportado, então não quebra quando o SVC atualizar — ao contrário de um mixin na
tela dele, que era a outra alternativa considerada.

> ⚠️ **Correção a uma afirmação antiga deste arquivo.** Dizia-se que "a API expõe acesso
> à configuração do servidor", como caminho plausível para desabilitar grupos por código.
> **É falso.** O `ConfigAccessor` da API tem só `hasKey`, `getValue`, `getString`,
> `getBoolean`, `getInt` e `getDouble` — nenhum setter. Dá para *ler* a configuração, não
> para mudá-la. Conferido com `javap` no jar da API.

**A segunda camada é a rede de segurança.** `VoicechatIntegration` continua cancelando
`CreateGroupEvent` e `JoinGroupEvent`, agora **explicando ao jogador** por que nada
aconteceu. Isso cobre o servidor onde ninguém configurou a opção — e cancelar sem
explicar era exatamente o defeito original: o botão parecia quebrado.

O cancelamento foi conferido no bytecode do jar, não por suposição:
`ServerGroupManager.addGroup` e `.joinGroup` chamam
`PluginManager.onCreateGroup`/`onJoinGroup` e retornam **antes** de criar o grupo ou
mandar o pacote de confirmação se algum plugin cancelou.

> ⚠️ A mensagem é enviada com `player.server.execute(...)`. O evento chega pela thread do
> Simple Voice Chat, e mandar pacote para um jogador de fora da thread do servidor é o
> tipo de coisa que passa no teste e quebra num servidor cheio.

### O que ainda não se sabe

- **Se a ligação deve vazar para quem está por perto.** Hoje não vaza: cancelar o pacote
  de microfone suprime a voz de proximidade no mesmo gesto que desvia o áudio. É o que o
  desenho pedia, mas é diferente de um telefone de verdade, e reverter é não cancelar.
- **Chamar quem está offline.** Não existe, e provavelmente não deve: recado para quem
  não está é o degrau 2 da aba social, que é um mod inteiro por si só.
- **Chamar quem nunca teve um poképhone.** Continua na lista e continua "tocando" do
  lado de quem ligou — só o aviso e o toque do lado de quem não tem como atender foram
  calados, porque avisar sem dar meio de agir só confundia. Tirar da lista de vez é outra
  decisão, adiada de propósito.

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

Os botões de canto já existem, e o menu já virou **grade de ícones** — três por linha,
como a tela inicial de um celular, nos dois aparelhos. Os ícones de hoje são caracteres da
fonte (◎ missões, ✉ social, ☎ ligações) e existem só para segurar o lugar até as texturas.

**Fixo ou nine-slice? A regra é: estica, nine-slice; não estica, fixo.**

| peça | como |
|---|---|
| ícone (o desenho) | **PNG fixo**, 32×32 |
| quadrado atrás do ícone | **PNG fixo** se todos forem do mesmo tamanho |
| moldura da tela | **nine-slice** — tem duas proporções, deitada e em pé |
| cartão de missão | **nine-slice** — a largura muda com a moldura |
| botão de largura variável | **nine-slice** |

Nine-slice onde não precisa é trabalho a mais no desenho (recortar bordas, escrever o
`.mcmeta`) sem ganho nenhum. E ícone esticado fica borrado, que é o defeito que o
nine-slice existe para evitar.

O resto é desenho, e o caminho técnico é o **sistema de sprites de GUI** da 1.21, não
`drawTexture` com coordenadas na mão:

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

## Apps e funcionalidades — levantamento de 24/09

> **Estado em 25/09:** todas as **baixas e médias** foram escritas na `main` — ver
> `PLANO.md`, sessão de 25/09. Ainda não compiladas nem vistas em jogo. Ficaram de fora:
> mensagens e mapa (média/alta), compartilhar fotos e missões cooperativas (alta),
> lanterna (alta sozinha; o caminho barato exige dependência nova), e **PC remoto** e
> **desafio de batalha**, que são médias mas nascem na branch `cobblemon`. O radar entrou
> na `main`, sem shiny nem lendário.

Lista do autor mais propostas, com complexidade estimada. **Baixa** cabe numa sessão;
**média** leva algumas; **alta** leva dias e tem risco de desenho, não só de código.
Nada aqui foi decidido — é inventário para escolher a ordem.

### Fundações — o que várias funcionalidades pedem ao mesmo tempo

Construir uma vez e reaproveitar. Sem elas, cada app reinventaria a sua.

- **Contatos** — *média*. Lista por jogador de quem ele salvou, com o nome guardado
  junto do UUID (para aparecer mesmo com o contato offline). Em servidor grande é o que
  impede a lista de ligações de mostrar todo mundo. Mais importante: vira a **camada de
  consentimento** — só contato recebe mensagem, vê sua localização ou te liga. Decidir:
  adicionar é unilateral ou exige aceite dos dois lados?
- **Atualização ao vivo** — *baixa/média*. Hoje o servidor manda um retrato das
  missões só ao abrir a tela (decisão Q6 da v2). Rastreio no HUD, notificações e
  mensagens exigem o servidor empurrar mudanças com a tela fechada. É reabrir aquela
  decisão, agora com motivo.
- **Central de notificações** — *baixa/média*. Um aviso único para missão concluída,
  ligação perdida, mensagem nova, alarme. Toast do vanilla ou camada própria no HUD.
- **Armazenamento de mundo** — *média*. O progresso de hoje mora no anexo do jogador,
  que só existe com ele conectado. Tudo que envolve jogador **offline** — mensagem para
  quem saiu, nome de contato, ranking — precisa de um `PersistentState` do mundo.
- **Campo de texto** — *baixa*. Notas, mensagens e nome de ponto de interesse usam o
  mesmo widget de edição. O vanilla tem um multilinha.
- **Camada de HUD** — *baixa*. Um único dono do que o mod desenha na tela durante o
  jogo (rastreio de missão, seta, notificações), para as coisas não se sobreporem.
- **Configuração do servidor** — *baixa*. Radar, compartilhar localização e fotos
  variam muito de servidor para servidor; o dono precisa poder desligar cada um.

### As ideias do autor

| Funcionalidade | Complexidade | O que pesa |
|---|---|---|
| Notas | baixa | campo de texto e anexo do jogador; limite de tamanho |
| Alarme / timer | baixa | tudo no cliente; alarme por hora do mundo ou real; precisa tocar com o aparelho fechado |
| Localização pelo chat | baixa | texto clicável que roda um comando nosso (`/pokebook rastrear x y z`) — o vanilla não tem ação de clique melhor que comando |
| Pontos de interesse | baixa/média | lista por jogador com nome, posição, dimensão e ícone |
| Seta apontando para o ponto | baixa/média | ângulo entre o olhar e o alvo, desenhado girado no HUD; decidir o que mostrar em outra dimensão |
| Rastreio de missão no HUD | baixa/média | depende da atualização ao vivo |
| Contatos | média | fundação — ver acima |
| Compartilhar localização / seguir | média | o cliente não sabe onde está quem está longe; o servidor tem de mandar a posição periodicamente, e só com consentimento. Em servidor de PvP é arma |
| Radar de Pokémon | média | só na branch `cobblemon`; limitado ao que o cliente carrega (distância de rastreio de entidades). Radar que mostra lendário e shiny desequilibra — configurável |
| Mensagens | média/alta | entrega para offline, histórico, limite de tamanho e de frequência; em servidor público o dono vai querer registro |
| Mapa da área próxima | média/alta | o "próxima" é o que o torna viável: o cliente já tem os chunks carregados e sabe a cor de cada bloco (`MapColor`). Gerar a imagem aos poucos e guardar em cache, senão trava |
| Fotos — galeria local | média | captura de tela do cliente, guardada no cliente |
| Fotos — compartilhar | alta | o limite de pacote cliente→servidor é da ordem de 32 KB, então a imagem vai em pedaços; armazenamento no servidor; e **moderação de imagem**, que num servidor público é problema real |

### Propostas a mais

- **Último local de morte** — *baixa*. Ponto de interesse criado sozinho ao morrer. A
  bússola de recuperação do vanilla faz isso, mas custa eco shard.
- **Relógio e clima** — *baixa*. Dia do mundo, fase da lua, e quanto falta para a
  chuva — o servidor sabe, o cliente não.
- **Não perturbe / modo avião** — *baixa*. Bloqueia ligações, mensagens e
  localização. Complemento natural dos contatos.
- **Papel de parede e capa** — *baixa*. Usa o que já existe de tingimento.
- **Acesso remoto ao PC do Cobblemon** — *média*, branch `cobblemon`. A pesquisa
  achou o `PCLinkManager`, feito para bloco customizado dar acesso ao PC. Só no
  pokébook: reforça que a estação administra.
- **Desafio de batalha pelo celular** — *média*, branch `cobblemon`.
- **Ranking de missões** — *média*. Precisa do armazenamento de mundo para incluir
  quem está offline.
- **Missões cooperativas** entre contatos — *alta*. Progresso compartilhado muda o
  modelo de dados.
- **Lanterna** — *alta* sozinho; *baixa* se houver mod de luz dinâmica instalado para
  integrar, na mesma fronteira de classe de sempre.

### Onde cada coisa mora

Segue a divisão já adotada — **a estação administra, o bolso comunica**:

- **Poképhone**: ligações, mensagens, seta e rastreio, radar, compartilhar
  localização, câmera, alarme, notificações.
- **Pokébook**: resgate de recompensa, PC remoto, ranking, mapa em tela grande.
- **Os dois**: contatos, pontos de interesse, notas.

### Ordem sugerida

1. **Fundações baratas**: contatos, atualização ao vivo, central de notificações.
2. **Ganhos rápidos**: pontos de interesse com seta, último local de morte, localização
   pelo chat, rastreio de missão no HUD, alarme, relógio e clima, notas.
3. **Social**: compartilhar localização, mensagens.
4. **Cobblemon**: radar, PC remoto, desafio de batalha.
5. **Pesados**: mapa, fotos, missões cooperativas.

## Outras pendências menores

- **Contorno cheio de linhas** — a escada de quatro caixas desenha quatro wireframes
  sobrepostos. Dá para separar: contorno simples com uma caixa, colisão detalhada com a
  escada. São métodos diferentes, não é preciso escolher.
- **Som ao clicar** — cortado do escopo da v1 de propósito.
- **Rolagem na lista de missões** — quando passarem de caber na moldura.
- **Missões repetíveis** — decidido: ver "Missões diárias sorteadas" acima.
