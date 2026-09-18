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
- **Rolagem na lista de missões** — quando passarem de caber na moldura.
- **Missões repetíveis** — decidido: ver "Missões diárias sorteadas" acima.
