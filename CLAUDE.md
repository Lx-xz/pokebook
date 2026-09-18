# CLAUDE.md

Contexto para sessões futuras do Claude Code neste projeto.

## O que é

Mod de Minecraft chamado **Pokébook**. Um bloco em formato de notebook cuja tela
acende ao ser clicado.

O destino do projeto **não é decoração**: é um **sistema de missões** ("capture 3
Pidgeys") com recompensas, integrado ao Cobblemon. O bloco, a interface e as missões
com alvos do vanilla já funcionam; falta o Cobblemon.

## Perfil do autor

O autor programa em outra linguagem, mas **nunca usou Java nem escreveu um mod**.
Explique o que é específico de Java e do ecossistema Minecraft; não precisa explicar
lógica, orientação a objetos ou controle de fluxo.

## Stack

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | Fabric + Fabric API |
| Java | JDK 21 |
| Mod id | `pokebook` |
| Pacote | `io.github.lxxz.pokebook` |
| Repo | `github.com/Lx-xz/pokebook` |

> O usuário do GitHub é `Lx-xz`, mas **hífen não é válido em pacote Java** — por isso
> o pacote é `lxxz`, sem hífen. Não "corrija" isso.

## Decisões de arquitetura (já fechadas, não reabrir sem motivo)

- **Sem BlockEntity.** O progresso das missões é **por jogador**, não por bloco. Dois
  jogadores no mesmo pokébook veem listas diferentes. O bloco é só um portal —
  permanece "burro", só com propriedades de blockstate. Isso continua valendo quando
  as missões chegarem.
- **A tela tem quatro níveis, não um booleano.** A propriedade é `SCREEN` (0 apagada,
  3 acesa); `LIT` não existe mais. Os intermediários existem para a animação de
  acender e apagar.
- **Luz escalonada por nível**: `0 / 2 / 5 / 7`. Se a luz caísse de 7 a 0 de uma vez
  enquanto a imagem desbota, o ambiente piscaria antes da tela terminar.
- **A animação usa ticks agendados de bloco encadeados**, não BlockEntity: cada tick
  anda um nível e agenda o próximo. É barato, é salvo com o chunk e sobrevive a
  recarregar o mundo. Cada passo **reconsulta o alvo** em vez de guardá-lo, para que
  reabrir no meio do fade-out inverta a animação em vez de terminar no lugar errado.
- **Tempos**: acender 2 ticks por nível; ao fechar, 3 s parada e depois 1 s por nível.
- **`SCREEN` guarda duas intenções**: "o jogador deixou aceso" e "alguém está com a
  tela aberta". A regra é **alvo = 3 se há espectador ou o manual está ligado, senão 0**.
  Enquanto ninguém olha e nada está em transição, o nível *é* o estado manual — é assim
  que ele persiste sem BlockEntity.
- **Interação só com mão vazia** — sai de graça usando `useWithoutItem`, que o jogo
  só invoca quando a mão principal está vazia. Não escreva um `if` para isso.
- **Template Fabric puro**, sem architectury nem Kotlin. O Cobblemon entra depois
  como repositório Maven + dependência, não exige re-scaffolding.

## Branches — leia antes de commitar

O repositório tem **duas branches permanentes**, e elas não são "estável e
experimental": a divisão é por **dependência**, e existe por uma restrição de rede.

| branch | o que tem |
|---|---|
| `main` | tudo, **menos** a dependência do Cobblemon |
| `cobblemon` | a `main` mais o `build.gradle`, o `gradle.properties`, a classe de integração e as missões de Pokémon |

**Por que:** com a dependência do Cobblemon na `main`, a máquina do trabalho não
conseguiria **nem buildar** o projeto — o firewall trava no jar de 141 MB (ver
"Ambiente do autor"). A separação é o que mantém as duas máquinas úteis: o Cobblemon em
casa, todo o resto em qualquer lugar.

**A regra de ouro:** mudança que **não** seja de Cobblemon **nasce na `main`** e é
trazida para a branch com `git merge main`. Nunca o contrário. Código que nascer na
`cobblemon` fica preso lá até um merge que não se quer fazer — a branch nunca volta para
a `main`, porque levaria a dependência junto.

> ⚠️ Isso já foi violado uma vez, por distração de estar com a branch em check-out.
> **Antes de commitar, confira `git branch --show-current`.** Se o commit não tocar
> `build.gradle`, `gradle.properties`, `integration/` ou as missões de Pokémon, ele
> pertence à `main`. Conserto: `git cherry-pick -x <sha>` para a `main`.

**O que mantém isso possível** é que o sistema de missões inteiro é livre do Cobblemon.
Nenhum tipo dele atravessa a fronteira: o que entra no `MissionTarget` é `Identifier` e
`String`. Uma missão de Pokémon **carrega** numa instalação sem o Cobblemon; ela apenas
nunca progride. Ao escrever coisa nova, preserve essa propriedade — se um `import
com.cobblemon` aparecer fora de `integration/`, a divisão quebrou.

## Armadilhas específicas deste projeto

**Modelos de bloco Java aceitam só 5 ângulos de rotação**: `0`, `±22.5`, `±45`.
O modelo já foi corrigido (estava em 10°, que o jogo rejeita). Se o autor reexportar
do Blockbench, **revalide os ângulos**.

**Caminhos de textura precisam do namespace**: `pokebook:block/chassi`, não
`block/chassi`. O Blockbench exporta sem o namespace — sempre confira após reexport.

**A 1.21 renomeou pastas de data para o singular**: `loot_table`, `recipe`,
`tags/block`. Usar o plural falha **silenciosamente** — a build passa, mas não
funciona no jogo.

**Sem loot table o bloco some ao ser quebrado**, sem dropar nada.

**`nonOpaque()` é obrigatório** — o modelo não preenche o cubo; sem isso as faces
dos blocos vizinhos desaparecem.

**O projeto usa mappings Yarn, não os oficiais da Mojang.** A documentação atual do
Fabric mostra os nomes da Mojang, que são diferentes: `onUse` e não `useWithoutItem`,
`Identifier` e não `ResourceLocation`, `nonOpaque` e não `noOcclusion`,
`addDrawableChild` e não `addRenderableWidget`. Copiar de lá dá nome inexistente.

**`Identifier` perdeu o construtor público na 1.21** — usar `Identifier.of(ns, path)`.

**Texto de interface é traduzido: dimensione pelo idioma mais verboso.** Um rótulo de
aba que cabia em "Active" saiu cortado em "Em andamento". Ao escolher largura de botão
ou de coluna, confira no português, não no inglês.

**Widget não convive com lista que rola.** Widget tem posição fixa e a lista muda de
posição a cada quadro. Numa lista rolável, desenhe as linhas à mão e trate o clique com
o deslocamento aplicado; guarde widgets para o que não rola.

## Sobre o Cobblemon (para quando as missões chegarem)

- Maven: `https://artefacts.cobblemon.com/releases/`
- Coordenadas: `com.cobblemon:fabric:1.8.0+1.21.1`
- MDK oficial com variante **em Java puro**: `gitlab.com/cable-mc/cobblemon-mdks`
- O código-fonte está no **GitLab**, não no GitHub (`Cobblemon/Cobblemon` no GitHub
  não existe, só forks)
- Cobblemon empacota o Fabric Language Kotlin no próprio jar — o usuário final não
  instala nada extra, e um mod em Java puro consome a API normalmente
- ⚠️ **Mas o ambiente de desenvolvimento precisa do plugin do Kotlin** —
  `org.jetbrains.kotlin.jvm`, mesmo sem uma linha de Kotlin no projeto. Ele é o que liga
  o remapeamento dos **metadados** do Kotlin no Loom. Sem ele o jogo **crasha na
  inicialização** com `ClassNotFoundException: net.minecraft.class_2960` — o nome
  intermediary de `Identifier`. O Loom remapeia o bytecode do jar, mas a reflexão do
  Kotlin resolve classes por strings nos metadados, que o remapeamento de bytecode não
  alcança. Ao aplicar o plugin, **limpe `.gradle/loom-cache/remapped_mods`**, senão o
  jar remapeado velho continua sendo usado
- `CobblemonEvents` usa `@JvmField` e sobrecargas `Consumer<T>`;
  **`POKEMON_CAPTURED` é o gancho central** do sistema de missões
- ⚠️ **A API quebra entre versões menores.** A MDK oficial fixa faixa estreita
  (`>=1.7.3 <1.8.1`) e o changelog da 1.8.0 lista remoções de classe. Todo update do
  Cobblemon exige recompilar.

## Ambiente do autor

Ele trabalha em duas máquinas. A do trabalho fica atrás de um **firewall Fortinet**,
que atrapalha de três formas distintas. As duas primeiras estão resolvidas:

1. **Inspeção de TLS** — sintoma `PKIX path building failed`. ✅ Resolvido: o CA da
   Fortinet foi importado no `cacerts` do JDK. O certificado está em
   `C:\Users\2699\corp-ca.pem`, e o `keytool` importa só o primeiro certificado por
   arquivo — esse PEM tem dois, então precisam ser separados e importados um a um.
2. **Filtro de URL** — `artefacts.cobblemon.com` responde **403** (corpo vazio, sem
   cabeçalho `Server`) a clientes que não são navegador. ✅ Contornado: use o espelho
   `https://maven.impactdev.net/repository/development/`, que serve os mesmos artefatos.
3. **Antivírus de gateway** — ⛔ **não resolvido, e provavelmente insolúvel sem o TI.**
   O FortiGate bufferiza o arquivo inteiro para escanear antes de liberar o primeiro
   byte. Arquivos até ~50 MB passam (o `minecraft-server.jar` de 49 MB baixou normal);
   o jar do Cobblemon, com **141 MB**, trava — a conexão nunca começa a responder.
   `HEAD` funciona, o POM funciona, o jar não.

**Consequência prática: não dá para buildar com Cobblemon na máquina do trabalho.**
Em casa nada disso acontece. Trabalho que dependa do Cobblemon fica para casa; desenho,
código que não o toca e assets podem ser feitos em qualquer uma.

O jar também nunca pode ir para o git — o GitHub rejeita arquivos acima de 100 MB.

## Sistema de missões

- **Sempre ativas**: não há aceitar nem recusar, o progresso conta sozinho.
- **Dois eixos de extensão**: `ObjectiveType` (o que fazer) e `TargetMatcher` (em
  quem vale). Separá-los permite combinar "derrotar" com "tipo voador" sem uma classe
  por combinação. O alvo trabalha sobre `Entity` porque Pokémon também são entidades.
- **Todas usam entidades vanilla, e isso é produto e não andaime.** É o que mantém o
  sistema verificável sem o Cobblemon, inclusive no dia em que um update dele quebrar a
  integração e for preciso saber de quem é o defeito.
- **Progresso na API de anexo do Fabric**, não no armazenamento do Cobblemon — a API
  dele quebra entre versões menores e não pode ter o poder de corromper histórico.
  `copyOnDeath()` é obrigatório: sem ele o respawn cria uma entidade nova e apaga tudo.
- **Definidas em datapack**, em `data/<namespace>/pokebook/mission/<nome>.json`. O id sai
  do caminho. As missões do próprio mod passam pelo mesmo caminho, como datapack
  embutido — **não há lista em Java**, e portanto não há um caso "de dentro" que funcione
  diferente do "de fora". Formato documentado no `README.md`.
- **Três objetivos × quatro alvos.** `kill`, `capture`, `battle_win` × entidade, espécie,
  tipo elemental, geração. Combinações novas saem sem código: só JSON. Foi para isso que
  os dois eixos existem.
- **Nome gerado, não escrito.** O título é montado de verbo + alvo + quantidade
  ("Capturar Pidgey ×3"); não há chave de tradução por missão. O formato é "alvo ×N"
  porque os arquivos de idioma do Minecraft **não têm plural** e o nome de uma espécie é
  sempre singular.
- **Resgate por botão, não automático.** Com recompensa automática o pokébook deixaria
  de ter função — ninguém precisaria abri-lo.
- **Inventário cheio recusa e mantém resgatável**, em vez de dropar aos pés. A
  recompensa é única, e item caído sobre lava ou no void some para sempre. É
  deliberadamente diferente do vanilla.

## Interface

- **`Screen` com pacote próprio**, não `ScreenHandler`. Este é feito para containers
  (slots e `PropertyDelegate` de inteiros), e em 1.21.1 ainda exigiria um access
  widener próprio.
- **A troca entre telas passa por `navigateTo`.** `removed()` dispara em qualquer
  troca — sem isso, ir do menu para as missões mandaria o pacote de fechamento e
  apagaria a tela no meio do uso.
- **O menu nasceu com um botão só de propósito**: a navegação define a forma dos
  pacotes, e encaixar um menu depois significaria mexer num fluxo já funcionando.

## Técnica

Quando um nome de API do Yarn não fechar, **leia os membros reais do jar remapeado** com
`javap`, em vez de tentar variações. O jar no cache do Gradle é a fonte de verdade — a
documentação oficial do Fabric hoje mostra mappings da Mojang, que dão nomes diferentes.

## Estado atual

v1, o sistema de missões e a integração com o Cobblemon estão prontos e verificados em
jogo. Veja `PLANO.md` para o histórico e `IDEIAS.md` para o que ainda é intenção.
