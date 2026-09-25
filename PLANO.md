# Pokébook — Plano e histórico

> **Para retomar numa conversa nova:** leia `CLAUDE.md` primeiro (contexto, decisões
> e armadilhas), depois este arquivo. O ponto exato onde paramos está no final, em
> **Onde paramos**.

> ⚠️ **As seções daqui até "Pendências da v1.1" são registro histórico do
> planejamento da v1**, mantidas pelo raciocínio que carregam. Várias decisões que
> elas descrevem **já foram substituídas** — a mais visível é o `LIT` booleano, hoje
> `SCREEN` com quatro níveis. Em caso de divergência, **o `CLAUDE.md` vale**.

## Contexto

Primeiro mod de Minecraft do autor, que programa em outra linguagem mas nunca usou
Java. O modelo do notebook já foi feito no Blockbench, com duas texturas de tela —
apagada e acesa — e a ideia é que a tela acenda ao clicar no bloco.

O destino declarado **não** é decoração: é um **sistema de missões** ("capture 3
Pidgeys") com recompensas, integrado ao Cobblemon. Esta v1 entrega o bloco interativo
funcionando de ponta a ponta. O objetivo é fechar um ciclo completo — obter o bloco,
colocar, clicar, ver acender — para aprender os padrões de registro do Fabric antes
de encarar uma API externa que quebra a cada update.

## Stack

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | Fabric (+ Fabric API) |
| Java | JDK 21 |
| Mod id | `pokebook` |
| Pacote | `io.github.lxxz.pokebook` |
| Repo | `github.com/Lx-xz/pokebook` |

## Decisões fechadas

Estas saíram de três rodadas de perguntas com o autor. Não reabrir sem motivo novo.

**Escopo** — v1 é decoração interativa; o destino é o sistema de missões. Escopo
**intermediário**: bloco + clique + aba no criativo + luz. **Sem** receita de craft e
**sem** som.

**Plataforma** — Fabric, a partir do template Fabric puro. Sem architectury, sem
Kotlin. O Cobblemon entra depois como repositório Maven + dependência; isso **não**
exige refazer o scaffold.

**Comportamento** — missões serão **por jogador**; tela fica acesa **até clicar de
novo**; luz **nível 7** aceso / 0 apagado; interação **só com a mão vazia**.

**Consequência arquitetural:** como o estado é por jogador e a tela é um interruptor,
o bloco nunca precisa de BlockEntity — só propriedades de blockstate. Sem tick
agendado, sem sincronização cliente-servidor. Isso continua válido na v2.

## O que falta construir

### 1. Projeto Gradle (raiz) — ✅ feito

`build.gradle`, `settings.gradle`, `gradle.properties`, wrapper (`gradlew`,
`gradlew.bat`, `gradle/wrapper/`), `.gitignore` já escritos, baseados no template
oficial `FabricMC/fabric-example-mod` na branch de 1.21.1.

Versões levantadas em 17/09/2026 (`https://meta.fabricmc.net/v2/versions/...` e
GitHub releases do `fabric-loom`):

| propriedade | valor |
|---|---|
| `minecraft_version` | `1.21.1` |
| `yarn_mappings` | `1.21.1+build.3` |
| `loader_version` | `0.19.5` |
| `loom_version` | `1.17.21` |
| `fabric_api_version` | `0.116.17+1.21.1` |
| Gradle wrapper | `9.5.1` |

> ⚠️ **Mudança no ecossistema desde que este plano foi escrito:** o template
> oficial `fabric-example-mod` migrou de Yarn para `loom.officialMojangMappings()`
> por padrão, e o plugin do Loom passou a ter dois ids — `net.fabricmc.fabric-loom`
> (sem remap, para versões não ofuscadas) e `net.fabricmc.fabric-loom-remap` (com
> remap, o nosso caso, já que o Minecraft 1.21.1 é ofuscado). **Decisão do autor:
> continuar com Yarn** (`mappings "net.fabricmc:yarn:${yarn_mappings}:v2"` no
> `build.gradle`), porque as mappings oficiais da Mojang não incluem nomes de
> parâmetro — só classe/método/campo — o que deixa o código-fonte decompilado do
> Minecraft mais difícil de ler para quem nunca escreveu Java. Isso também exigiu
> subir o Gradle wrapper para a 9.5.1, que o `loom_version` 1.18.2 requer.
>
> O `build.gradle` do template também trazia `splitEnvironmentSourceSets()`, um
> segundo entrypoint de cliente e um bloco `mixins` — removidos, porque o escopo
> desta v1 (ver "O que falta construir" abaixo) não usa nenhum dos três.

> ⚠️ **Loom 1.18.x exige JVM 25 para rodar o Gradle** (não é a versão de bytecode do
> mod — é o runtime do processo de build em si). Com JDK 21 instalado, o build falha
> na fase de configuração com `Dependency requires at least JVM runtime version 25`.
> Por isso o `loom_version` ficou em `1.17.21` — última linha estável do Loom
> compatível com JDK 21 — em vez do 1.18.2 mais recente. Se um dia o autor migrar
> para JDK 25+, o 1.18.x volta a ser opção.

`gradlew build` **validado com sucesso** em 17/09/2026 (`BUILD SUCCESSFUL`, sem
código Java ainda — só confirma que o Gradle/Loom resolve as dependências e gera o
jar vazio). Dois avisos do próprio Gradle, sem ação necessária por ora:
- O projeto está dentro do OneDrive, o que o Gradle/Loom aponta como fonte
  conhecida de lentidão e possíveis problemas (sincronização mexendo em arquivos
  que o build está escrevendo). Não bloqueou o build; vale lembrar se aparecerem
  travamentos estranhos mais para frente.
- O primeiro build esperou ~2min por um lock do cache do Loom seguro pela extensão
  Java do VS Code (`redhat.java`), que também estava rodando Gradle no mesmo
  projeto. Não é erro — só contenção entre dois processos Gradle simultâneos.

### 2. Código Java — `src/main/java/io/github/lxxz/pokebook/` — ✅ feito

> ⚠️ **Este plano foi escrito com os nomes das mappings oficiais da Mojang**
> (`useWithoutItem`, `BlockStateProperties`, `noOcclusion`, `ResourceLocation`,
> pacotes `net.minecraft.world.level.block.*`). O `build.gradle` usa **Yarn**, onde
> os mesmos membros se chamam `onUse`, `Properties`, `nonOpaque`, `Identifier`, em
> `net.minecraft.block.*`. O código foi escrito em Yarn; a descrição abaixo já está
> corrigida.

**`Pokebook.java`** — `ModInitializer`. Constante `MOD_ID = "pokebook"`, um `Logger`
do SLF4J, e `onInitialize()` chamando `ModBlocks.register()` antes de
`ModItemGroups.register()` (a aba usa o bloco como ícone e como entrada).

**`block/PokebookBlock.java`** — estende `Block`, concentra o comportamento:

- Propriedades `FACING` (`Properties.HORIZONTAL_FACING`) e `LIT` (`Properties.LIT`),
  declaradas em `appendProperties`
- `getPlacementState` → `ctx.getHorizontalPlayerFacing().getOpposite()`, para a tela
  nascer virada ao jogador
- `onUse(...)` (a sobrecarga de 5 parâmetros, **sem** `ItemStack`) →
  `world.setBlockState(pos, state.cycle(LIT), Block.NOTIFY_ALL)`. Esta sobrecarga
  **só é invocada com a mão vazia** — a regra de interação sai de graça, sem nenhum
  `if`. Age só no servidor (`!world.isClient`) e devolve `ActionResult.SUCCESS`
- `getOutlineShape` → `VoxelShape` própria (a colisão segue a outline por padrão); o
  modelo não preenche o cubo. Duas caixas — base e tela — descritas para `facing=north`
  e giradas por código para as outras três direções, no mesmo sentido horário do `y`
  do blockstate

**`registry/ModBlocks.java`** — registra bloco e `BlockItem` via
`Registry.register(Registries.BLOCK, ...)`. Propriedades: `strength(1.5f)`,
`sounds(BlockSoundGroup.METAL)`, `nonOpaque()`, `luminance(s -> s.get(LIT) ? 7 : 0)`.

**`registry/ModItemGroups.java`** — aba própria no criativo via
`FabricItemGroup.builder()`, com o pokébook como ícone. A chave
`itemGroup.pokebook.main` já existe nos arquivos de lang.

### 3. Recursos — `src/main/resources/`

- **`fabric.mod.json`** — ✅ feito (id, versão, entrypoint para `Pokebook`, `depends`
  em `fabricloader`, `minecraft` `~1.21.1`, `java` `>=21`, `fabric-api`)
- **`data/pokebook/loot_table/blocks/pokebook.json`** — ✅ feito. Sem isto **o bloco
  some ao ser quebrado**, sem dropar nada
- **`data/minecraft/tags/block/mineable/pickaxe.json`** — ✅ feito. Quebrável com
  picareta

### Já prontos — não mexer

`assets/pokebook/`: `blockstates/pokebook.json` (8 variantes facing × lit),
`models/block/pokebook.json` + `pokebook_on.json`, `models/item/pokebook.json`,
`lang/en_us.json` + `pt_br.json`, e as 5 texturas.

## Fora de escopo nesta v1

Receita de craft · som ao clicar · qualquer integração com o Cobblemon · GUI ·
sistema de missões · renderização emissiva da tela.

## Verificação

1. `gradlew build` compila sem erro
2. `gradlew runClient` sobe o jogo (primeira vez: 5–15 min)
3. Existe uma aba "Pokébook" no criativo, com o item e o modelo corretos
4. Ao colocar, o bloco nasce **com a tela virada para o jogador**
5. Clicar com a mão vazia: a tela acende e o entorno ilumina
6. Clicar segurando um bloco: **não** alterna — coloca o bloco
7. Clicar de novo com a mão vazia: apaga
8. Quebrar com picareta: dropa o item
9. Log sem nenhum aviso de modelo ou textura faltando

### Riscos conhecidos

- **Orientação da tela** (passo 4) é suposição derivada da leitura do modelo: a face
  `north` do elemento da tela usa a textura `#1`. Se nascer de costas, girar os
  valores `y` do blockstate em 180°. Só dá para confirmar em jogo.
- **Pastas de data no singular** (1.21) — erro silencioso, a build passa e nada
  funciona.
- **`VoxelShape`** vai precisar de ajuste depois de ver o bloco no jogo.

## Passos manuais do autor

1. Instalar o JDK 21 — `winget install EclipseAdoptium.Temurin.21.JDK`
2. Tornar o repo `Lx-xz/pokebook` privado (Settings → General → Danger Zone)
3. `git init` local e conectar ao remote

---

## Verificação em jogo — resultado (17/09/2026)

`gradlew runClient` rodou e a v1 **funciona de ponta a ponta**. Resultado item a item
da lista de Verificação acima:

| # | Item | Resultado |
|---|---|---|
| 1 | `gradlew build` | ✅ |
| 2 | `gradlew runClient` | ✅ |
| 3 | Aba "Pokébook" no criativo | ✅ aba existe, item aparece — mas o **ícone sai achatado** (ver P1) |
| 4 | Nasce virado ao jogador | ✅ o risco de orientação **não** se concretizou; o `getOpposite()` está certo |
| 5 | Clique com mão vazia acende | ✅ |
| 6 | Clique com item não alterna | ✅ |
| 7 | Clique de novo apaga | ✅ |
| 8 | Quebra com picareta e dropa | ✅ |
| 9 | Log sem avisos | a confirmar em `run/logs/latest.log` |

## Pendências da v1.1

Levantadas pelo autor olhando o bloco em jogo. Nenhuma impede a v1 de funcionar.

**P1 — O item não tem transformações de exibição.** ✅ **corrigido e validado em jogo.** Sintoma triplo, uma causa só:
ícone achatado no inventário e na hotbar, item invisível em primeira pessoa, e
tamanho/posição errados na mão em terceira pessoa. O modelo exportado do Blockbench
(`models/block/pokebook.json`) **não declara `parent`**, então não herda o bloco
`display` do `minecraft:block/block`, que é quem define escala e rotação do item em
cada contexto (`gui`, `firstperson_righthand`, `thirdperson_righthand`, `ground`,
`fixed`). Sem ele, o jogo usa a transformação identidade — o modelo fica em tamanho
natural e fora de enquadramento.

> Corrigido declarando o `display` **no `models/item/pokebook.json`**, não no modelo
> de bloco: o de bloco é o que o autor reexporta do Blockbench, e um reexport
> apagaria o ajuste; o de item é escrito à mão e sobrevive.

Foram cinco rodadas, todas no `models/item/pokebook.json`:

1. Declarar o bloco `display` com os valores do `minecraft:block/block`. Resolveu
   **dois dos três sintomas** — ícone isométrico no inventário/hotbar
   (`v1.1-pokebook-inventario.png`) e item correto na mão em terceira pessoa
   (`v1.1-pokebook-mao-3a-pessoa.png`). Dois caírem juntos confirmou que o `display`
   estava sendo lido.
2. Primeira pessoa continuava invisível. Causa: as transformações giram e escalam em
   torno do centro do cubo, `(8, 8, 8)`, e este modelo não preenche o cubo — a base
   tem 1 pixel de altura e quase toda a massa fica abaixo da metade, então caía fora
   do enquadramento. `translation [0, 4.5, 1]` e escala `0.5` o trouxeram de volta,
   mas grande demais e cortado, visto de cima e por trás.
3. Escala `0.35` e `y` girado 180° (`45` → `225` na mão direita, espelhado na
   esquerda) para a tela encarar a câmera. **Aprovado**
   (`v1.1-pokebook-mao-1a-pessoa.png`): reconhecível de relance, que era o critério.
   Ele encosta na borda direita, mas isso é normal para item segurado — blocos
   vanilla também saem parcialmente do quadro ali.
4. Ajuste a pedido do autor: espelhar a vista para a tela ficar **à direita**.
   `225` → `135` na mão direita. **Aprovado em jogo.**
5. A mão esquerda, porém, ficou **de costas**. Causa: seguiu-se a convenção do
   vanilla, em que `firstperson_lefthand` é o `righthand` **mais 180°**. Isso só
   funciona no vanilla porque blocos são **cubos simétricos** — girar um cubo 180°
   não muda nada. Este modelo tem frente e costas, então o +180 mostrou as costas.
   Corrigido para `225`: junto com o `135`, são 180° ∓ 45°, as duas vistas de 3/4
   **ambas** com a tela voltada para a câmera.

> Os quatro contextos restantes (`gui`, `ground`, `fixed`, `thirdperson_righthand`)
> são cópia literal do vanilla; só os dois de primeira pessoa têm valores empíricos.
> A translação é aplicada **depois** da rotação, no espaço já girado — girar 180°
> podia ter invertido o efeito do `translation z`, mas não inverteu.
>
> **Lição geral:** convenções do vanilla que pressupõem simetria (como
> `lefthand = righthand + 180`) não valem para modelos com frente e costas. O
> `__comment` dentro do `models/item/pokebook.json` registra isso no próprio arquivo.

**P2 — A hitbox não acompanha a inclinação da tela.** ✅ **resolvido** (commit
`8d6b807`): o modelo foi deslocado 2 px e a tampa aproximada por uma escada de
caixas. Ficou pendente só um detalhe estético, registrado em `IDEIAS.md`: a escada
desenha quatro wireframes sobrepostos no contorno. Dá para separar contorno simples
de colisão detalhada — são métodos diferentes, não é preciso escolher. Diagnóstico
original: a `VoxelShape` só aceita caixas
alinhadas aos eixos, e a tela do modelo é inclinada 22,5°. Hoje ela é uma caixa reta
única, folgada. Dá para aproximar a diagonal empilhando três ou quatro caixas finas
em degrau. Nunca vai encostar perfeitamente — é uma limitação do formato, não do
código.

**P3 — A tela acesa não parece acesa o bastante.** ⏳ **ainda aberto**, agora em
`IDEIAS.md`. A luz do bloco funciona (hoje escalonada `0/2/5/7` pelos quatro níveis
de `SCREEN`, ver `CLAUDE.md`), mas a textura da tela continua sendo sombreada pela iluminação da
cena, então no escuro ela escurece junto. O que dá a aparência de "ligada de verdade"
é **renderização emissiva** — explicitamente fora do escopo da v1. Duas saídas:
clarear a textura `screen_on.png` (barato, meia solução) ou renderização emissiva
(a solução real, mais trabalho).

**P4 — Registrar as capturas de tela.** ✅ feito. Ficam em `docs/screenshots/`, na
convenção **`v<versão>-pokebook-<assunto>.png`** — ex.: `v1-pokebook-inventario.png`
e `v1.1-pokebook-inventario.png`. Manter o `<assunto>` idêntico entre versões faz as
duas capturas da mesma cena ficarem lado a lado na listagem, virando um
antes-e-depois comparável. Capturas coladas no chat chegam ao Claude renderizadas,
não como arquivo: quem salva o PNG é o autor (`F2` no jogo escreve em
`run/screenshots/`).

## Onde paramos

### Sessão de 20/09/2026 — a moldura do poképhone

O autor desenhou a moldura em pixel art e ela está **ligada e verificada em jogo**. O
caminho até ela rendeu mais armadilhas que código; as duradouras foram para o `CLAUDE.md`.

**A moldura.** `pokephone_gui.png`, nine-slice, borda `{7, 6, 7, 26}`. O queixo de 26 px
foi escolha do autor, para caber o botão central de 14×14 com folga — dos 26, vinte são
chassi escuro e o resto é friso e sombra.

**O arquivo tem 256×256 e quase toda essa área é faixa uniforme.** Não é desperdício: é
o que evita a emenda do ladrilhamento (ver `CLAUDE.md`). As faixas foram geradas por
script a partir da arte de 64×64 do autor, repetindo colunas e linhas do miolo — nenhum
pixel foi redesenhado.

**`CONTENT_INSET` virou quatro constantes**, `INSET_LEFT/TOP/RIGHT/BOTTOM`. Um valor só
funcionava enquanto a moldura era simétrica, e ela deixou de ser: o aparelho tem queixo
grosso e testa fina, como um celular.

**Rótulo encolhe em vez de cortar, e agora num lugar só.** `PokebookScreenBase
.drawFittedLabel` — os ícones da tela inicial já faziam isso e as abas das missões não,
então "Resgatar" aparecia como "esgata". O `TabButtonWidget` novo sobrescreve só o
desenho do texto do botão do vanilla e herda o resto.

**O botão central existe e funciona**: volta para a tela inicial, e na tela inicial
desliga. Tem par de cores próprio (`COLOR_CHASSIS`/`COLOR_CHASSIS_HOVER`), tirado da
própria moldura, porque a paleta do mod foi escolhida para fundo claro e sumiria no
chassi escuro.

**Cada aparelho fecha de um jeito só.** Celular pelo botão do queixo, notebook pelo ✕.
Esc continua fechando os dois.

**A próxima coisa é uma interface própria para o notebook.** A moldura de hoje é a mesma
nos dois, e um notebook em paisagem com queixo de celular não é o desenho certo. Nada
começou.

**Continua em aberto**, sem nada feito: as abas ainda usam a moldura cinza do vanilla,
que é o que mais destoa da arte; os ícones das missões ainda são maçãs provisórias; falta
pixel art para cartão de missão, botão genérico, abas e barra de rolagem; e a entrevista
sobre o que está no `IDEIAS.md` nunca aconteceu.


### Sessão de 18/09/2026

Antes: correção do `AttachmentRegistry.builder()` `@Deprecated`, missões por datapack,
receita e renderização emissiva — detalhes nas seções acima e no `README.md`. Tudo isso
foi **verificado em jogo** e funciona.

Depois, a partir do que o autor viu na tela:

**Nomes de missão gerados.** Uma missão nova sem chave de tradução aparecia com o id
cru na interface (`mission.pokebook.one_enderman`). A chave por missão foi **removida**:
o nome agora é montado das peças que toda missão já tem — verbo do objetivo, alvo e
quantidade. As chaves passaram a ser por **objetivo** (`objective.pokebook.kill`), que
são poucas e fixas.

> ⚠️ O formato é "Abater Zumbi ×5", não "Abater 5 zumbis". Os arquivos de idioma do
> Minecraft **não têm plural**, e o nome que o jogo dá a uma entidade é sempre singular:
> "Abater 5 Zumbi" sairia errado em qualquer idioma. O "×N" depois do alvo evita o
> problema em vez de tentar resolvê-lo. `title` no JSON continua sobrescrevendo tudo,
> para a missão que merecer nome próprio.

**Botões de canto.** Fechar (✕) no canto superior direito e voltar (←) no esquerdo, em
**todas** as telas. Ficam na classe base, não em cada tela: "onde fica o X" é decisão de
uma vez só, e uma tela nova nasce com os dois no lugar. O `init()` da base virou
`final` e chama `initPanel()`, que é o que cada tela implementa.

> A tela de destino do "voltar" é criada **no clique**, não guardada: precisa nascer com
> os dados do momento, não com os de quando a tela atual abriu.

**Cobblemon — esta é a branch `cobblemon`.** As mudanças de build estão aqui, o jar de
141 MB baixou e o jogo sobe com ele. Elas **não** entram na `main`, e a razão é prática:
com o Cobblemon na `main`, a máquina do trabalho não conseguiria nem buildar o projeto — o FortiGate trava no jar (ver `CLAUDE.md`). A
separação é o que mantém a `main` utilizável nas duas máquinas, e é o que permite fazer
o redesenho da interface no trabalho enquanto a integração com o Cobblemon espera por
casa.

Por ora é só dependência de teste: **nenhuma classe do Cobblemon é mencionada no
código**, e por isso ele também não entrou no `depends` do `fabric.mod.json`.

> ⚠️ **A armadilha que isso custou**, registrada também no `CLAUDE.md`: o primeiro
> `runClient` com o Cobblemon **crashou na inicialização** com
> `ClassNotFoundException: net.minecraft.class_2960` — o nome *intermediary* de
> `Identifier`, que não existe em dev, onde as classes têm nomes Yarn. O Loom remapeia o
> bytecode do jar, mas o Cobblemon é Kotlin e resolve classes por **reflexão**, a partir
> de descritores guardados como **string** nos metadados — e strings não são bytecode.
>
> A correção é aplicar o plugin `org.jetbrains.kotlin.jvm` (2.2.20, a versão da MDK
> oficial), que liga o remapeamento de metadados Kotlin no Loom. **Não é para escrever
> Kotlin**, e a suposição de que ele só seria necessário ao compilar contra a API estava
> errada: ele já é necessário para apenas *carregar* o mod. Ao aplicá-lo, foi preciso
> limpar `.gradle/loom-cache/remapped_mods`, senão o jar remapeado antigo continuaria em
> uso.
>
> Verificado com `javap -v` na classe exata que crashou
> (`SpeciesAdditions$AdditionParameterAdapter`): zero ocorrências de `class_2960` e 13
> de `net/minecraft/util/Identifier`.

Tudo verificado em jogo.

### Integração com o Cobblemon — feita (nesta branch)

A captura conta progresso. `ObjectiveType.CAPTURE` e um alvo por espécie entraram, e com
eles o **segundo caso concreto** de `TargetMatcher` — o eixo de alvos deixou de ter uma
implementação só.

**O desenho mudou por uma assimetria da API.** `POKEMON_CAPTURED` entrega um
`Pokemon` — o objeto de dados — e não a entidade, que a essa altura já saiu do mundo.
Um alvo que só soubesse olhar `Entity` não decidiria nada sobre uma captura. Daí o
`MissionTarget`, que carrega o que se sabe em cada caso: a entidade, ao matar; o id da
espécie, ao capturar.

> A peça que faz tudo funcionar é a **espécie viajar como `Identifier` simples**. Com
> isso, `SpeciesMatcher` e todo o resto do sistema de missões **não mencionam o
> Cobblemon** e vivem na `main` — compilam na máquina do trabalho. Só
> `CobblemonIntegration` importa o mod, e é a única classe que precisaria de conserto
> quando a API quebrar num update.

Uma missão de captura **carrega numa instalação sem o Cobblemon**; ela apenas nunca
progride, em vez de quebrar o carregamento do datapack.

**Fronteira de classe, não condicional.** A JVM resolve referências ao *carregar* a
classe, então um `if (temCobblemon)` dentro de um método já falharia — a classe que o
contém não carregaria. É o terceiro lugar onde este padrão aparece no projeto.

**Sons emprestados do Cobblemon** (`pc.on`, `pc.off`, `gui.click`), procurados no
registro **por id**: nenhum arquivo dele é copiado para o nosso jar — copiar seria
redistribuir asset alheio, referenciar não é — e nenhuma classe dele é mencionada, então
isso também vive na `main`. Sem o Cobblemon, a busca devolve `null` e o mod fica em
silêncio. O som toca só no primeiro passo de cada transição; um por nível viraria
metralhadora.

**Nada disso foi visto em jogo.**

### Abas, rolagem e os três objetivos — feito

**A lista estourou a moldura** ao chegar na quinta missão. A correção não foi rolagem
sozinha: as **abas por estado** — em andamento, a resgatar, concluídas — foram o que
resolveu o problema de fundo, porque tiram da lista principal justamente as linhas que
não pedem nada.

> **Por que por estado e não por assunto.** Toda missão está num dos três estados, e
> separar assim põe a única coisa acionável — resgatar — numa aba própria. Abas por
> assunto (matar, capturar, derrotar) seriam **taxonomia**: organizam, mas não dizem o
> que fazer a seguir. Com poucas missões, taxonomia é enfeite. Se um dia forem dezenas,
> aí um filtro por assunto *dentro* de cada aba de estado faz sentido — é adição, não
> troca.

Filtro no **cliente**, como decidido: o servidor já manda tudo e cada linha já carrega o
que a classifica, então trocar de aba não toca a rede.

**As linhas deixaram de ser widgets.** Widget tem posição fixa, e numa lista que rola a
posição muda a cada quadro — um widget por linha exigiria reposicionar todos a cada
rolagem. Desenhar à mão e tratar o clique com o deslocamento aplicado é menos código e
não pode dessincronizar. As abas, que não rolam, continuam widgets. `enableScissor`
recorta a lista para a linha que sai por cima não invadir o título.

**Três objetivos e quatro alvos**, que é o eixo desenhado lá atrás finalmente pagando:
`KILL`, `CAPTURE` e `BATTLE_WIN` × entidade, espécie, tipo elemental e geração. Cinco
missões de exemplo cobrem cada combinação nova sem uma linha de Java por missão.

> **Geração é etiqueta, não campo.** O Cobblemon não guarda número de geração: guarda
> rótulos na espécie, entre eles `gen1`. Modelar como etiqueta espelha o dado real em vez
> de inventar um paralelo — e abre `legendary` e `paradox` de graça, se um dia quisermos.

> **Tipo elemental usa `showdownId`, não `getName()`.** O segundo é nome de exibição e
> mudaria com o idioma, fazendo a mesma missão casar numa máquina e não noutra.

> **Captura selvagem também encerra batalha em vitória.** Sem o `getWasWildCapture()`, a
> mesma ação contaria duas vezes: como captura *e* como vitória.

**Nada disso foi visto em jogo.**

**Próxima ação:** `gradlew runClient` nesta branch e conferir:

1. As três abas, e que a lista rola quando passa de quatro linhas.
2. Resgatar dentro de uma lista rolada acerta a missão certa.
3. Capturar um Pokémon de fogo avança `catch_fire_type`; vencer batalha contra um de
   água avança `beat_water_type`; e os de geração 1.
4. Capturar um Pokémon selvagem **não** avança nenhuma missão de `battle_win`.
5. Na `main`, sem o Cobblemon, tudo sobe — só sem som e sem progresso de Pokémon.

Depois: o **redesenho visual** (sprites com nine-slice, cara de macOS) e a **aba social,
degrau 1**.

Nota de ambiente: além do `PATH` de terminais antigos, a máquina tem um **JRE 8 da
Oracle** cujo atalho (`C:\Program Files (x86)\Common Files\Oracle\Java\java8path`)
fica no `PATH` do sistema **antes** do Temurin. Se o `JAVA_HOME` não estiver visível
na sessão, o `gradlew` cai nesse Java 8 e falha com *"Gradle requires JVM 17 or
later"*. Terminal novo resolve. Não fixar `org.gradle.java.home` no
`gradle.properties`: é caminho absoluto e o arquivo é versionado, indo para a outra
máquina do autor.

ubo, `(8, 8, 8)`, e este modelo não preenche o cubo — a base
   tem 1 pixel de altura e quase toda a massa fica abaixo da metade, então caía fora
   do enquadramento. `translation [0, 4.5, 1]` e escala `0.5` o trouxeram de volta,
   mas grande demais e cortado, visto de cima e por trás.
3. Escala `0.35` e `y` girado 180° (`45` → `225` na mão direita, espelhado na
   esquerda) para a tela encarar a câmera. **Aprovado**
   (`v1.1-pokebook-mao-1a-pessoa.png`): reconhecível de relance, que era o critério.
   Ele encosta na borda direita, mas isso é normal para item segurado — blocos
   vanilla também saem parcialmente do quadro ali.
4. Ajuste a pedido do autor: espelhar a vista para a tela ficar **à direita**.
   `225` → `135` na mão direita. **Aprovado em jogo.**
5. A mão esquerda, porém, ficou **de costas**. Causa: seguiu-se a convenção do
   vanilla, em que `firstperson_lefthand` é o `righthand` **mais 180°**. Isso só
   funciona no vanilla porque blocos são **cubos simétricos** — girar um cubo 180°
   não muda nada. Este modelo tem frente e costas, então o +180 mostrou as costas.
   Corrigido para `225`: junto com o `135`, são 180° ∓ 45°, as duas vistas de 3/4
   **ambas** com a tela voltada para a câmera.

> Os quatro contextos restantes (`gui`, `ground`, `fixed`, `thirdperson_righthand`)
> são cópia literal do vanilla; só os dois de primeira pessoa têm valores empíricos.
> A translação é aplicada **depois** da rotação, no espaço já girado — girar 180°
> podia ter invertido o efeito do `translation z`, mas não inverteu.
>
> **Lição geral:** convenções do vanilla que pressupõem simetria (como
> `lefthand = righthand + 180`) não valem para modelos com frente e costas. O
> `__comment` dentro do `models/item/pokebook.json` registra isso no próprio arquivo.

### Aba social, degrau 1 — feito

Ver o progresso dos outros. Como previsto, nenhum sistema novo: o dado já era persistido
por jogador, e isto é uma consulta e uma tela.

**Só jogadores conectados**, e isso é limitação consciente: o progresso de quem está
offline mora no arquivo de save do jogador, e lê-lo exigiria abrir um arquivo por jogador
a cada consulta. Placar histórico é outra funcionalidade.

**Resumo e não detalhe** — "quantas de quantas", com barrinha de progresso para a
comparação se ler de relance. Não cresce com o número de missões.

**Pedido sob demanda**, ao clicar na aba, e não junto ao abrir o pokébook: a maioria das
aberturas nunca chega ali, e mandar a lista de todos sempre seria pagar sempre por algo
usado às vezes. O custo é a tela nascer vazia até a resposta chegar.

Os dois receptores de cliente conferem a tela atual antes de aplicar a resposta — ela
pode chegar depois de o jogador ter navegado para outro lugar.

**Também nesta rodada:** a espera de 3 s antes de apagar a tela saiu (fechar a interface
e o bloco seguir aceso parecia defeito, não estilo), e o `CLAUDE.md` ganhou a seção de
**branches**, com a regra de que mudança sem Cobblemon nasce na `main`.

**Verificado em jogo, nas duas branches.** A aba social abre e lista, a tela apaga na
hora, e as abas de missão não cortam mais o texto.

### Poképhone, em versão de teste

Item registrado, abrindo **a mesma interface em pé**. Usa o modelo do pokébook até ter o
seu — o autor vai modelar.

**Custou pouco, e o motivo é uma decisão de dois anos-luz atrás**: o progresso sempre foi
**por jogador**, nunca por bloco. Celular e notebook mostram o mesmo dado sem uma linha
de sincronização.

**O que a tela em pé exigiu:** `PANEL_WIDTH`/`PANEL_HEIGHT` eram **constantes**. Viraram
`panelWidth()`/`panelHeight()`, respondidos pela sessão. Junto veio o `PokebookSession`,
que reúne o que as telas passavam solto de construtor em construtor — e concentra num
lugar só a diferença entre os dois aparelhos.

> `pos` ausente na sessão significa **aparelho de bolso**, e disso decorre tudo: não há
> bloco a avisar ao fechar, não há distância máxima — ninguém se afasta do próprio bolso
> — e a moldura é em pé. Portátil e em pé andam juntos hoje porque só há dois aparelhos;
> se um dia divergirem, viram dois campos.

**A arte da moldura é esticada** para a proporção em pé. Aceitável porque é um retângulo
de cor sólida com borda, e provisório: o redesenho com nine-slice resolve de verdade.

**Resgate só no pokébook**, conferido **no servidor**. Esconder o botão no cliente é
aparência — o pacote de resgate continua sendo um pacote que qualquer cliente pode
mandar. A autorização reusa o conjunto de espectadores que já existia para a animação.

**Receita:** um pokébook, ouro, redstone e ametista. ⚠️ **Consome o pokébook** — decisão
em aberto, registrada no `IDEIAS.md`.

**Também nesta rodada:** a receita do pokébook agora se desbloqueia ao obter ferro,
redstone **ou** vidraça (as condições ficam num único grupo de `requirements`, que é OU e
não E), e o `CLAUDE.md` teve conteúdo restaurado — uma "junção de seções" minha havia
apagado a seção nova em silêncio.

**Nada disso foi visto em jogo.**

### Branch `voicechat` — Simple Voice Chat no ambiente

Terceira branch, pelo mesmo critério das outras duas: **divisão por dependência**. O
`CLAUDE.md` tem a regra — mudança que não é da dependência nasce na `main`.

**Boa notícia para a máquina do trabalho:** o jar do Simple Voice Chat tem **5,1 MB**.
O que travou o Cobblemon era o tamanho (141 MB), e um jar de 49 MB já passou pelo
FortiGate. Esta frente deve ser tocável no trabalho, ao contrário da do Cobblemon.

**Duas versões, de propósito diferentes:**

| | |
|---|---|
| `voicechat_api_version=2.5.31` | `modCompileOnly` — só para compilar |
| `voicechat_mod_version=fabric-1.21.1-2.6.10` | `modRuntimeOnly` — o mod de verdade, só no teste |

Compila-se contra a API mais **antiga** que tenha o necessário e roda-se com o mod mais
**novo**: é a direção suportada, e a recomendação do próprio autor do SVC. O contrário
quebra. A API é `compileOnly` porque quem a fornece em jogo é o mod, que a empacota —
declará-la como `implementation` faria o jar dela viajar dentro do nosso.

**Dois repositórios**, porque os artefatos moram em lugares diferentes: o maven do autor
(`maven.maxhenkel.de`) serve só a API; o mod em si só está no maven do Modrinth.

**A fronteira de classe sai de graça aqui.** `VoicechatIntegration` implementa
`VoicechatPlugin` e é declarada no entrypoint `voicechat` do `fabric.mod.json` — quem a
carrega é o **próprio Simple Voice Chat**. Sem ele instalado, ninguém lê esse entrypoint
e a classe nunca é tocada. Não precisou de nenhum `if`, ao contrário do Cobblemon.
Dependência declarada em `suggests`, não em `depends`.

### A ligação — o telefone e o fio

Escrita em **duas metades**, e a divisão não é organização: é a regra de branch aplicada
ao código.

| metade | onde | branch a que pertence |
|---|---|---|
| o **telefone** — quem liga para quem, tocar, atender, recusar, desligar, desistir | `call/`, `network/`, `client/call/`, `CallScreen` | **`main`** |
| o **fio** — o áudio indo de um para o outro | só `integration/VoicechatIntegration` | `voicechat` |

A metade de cima **não menciona o Simple Voice Chat**. Os dois se falam por uma pergunta
só: `CallService.peerOf(uuid)` devolve um `UUID` ou `null`. É a mesma fronteira do
`MissionTarget` com o Cobblemon, pelo mesmo motivo — e é o que permite a sinalização
inteira ser levada para a `main` com `git cherry-pick -x`.

> Os dois commits desta frente são separados **de propósito**: o primeiro é o telefone e
> está pronto para a `main`; o segundo é o fio e fica aqui.

**O que a API do SVC dá, e o mecanismo:** enganchar `MicrophonePacketEvent`, **cancelá-lo**
— o que suprime a voz de proximidade daquele pacote — e reenviar como *static sound
packet* só para a conexão do destinatário. São de fato umas 10 linhas. O resto é nosso.

⚠️ **O cancelamento tem uma consequência de desenho:** em ligação, quem está por perto
**não ouve** este jogador. Um telefone de verdade deixa a sala ouvir metade da conversa.
Fica assim porque é o que o desenho do `IDEIAS.md` pede — a ligação é canal fechado — mas
é reversível: bastaria mandar o pacote e **não** cancelar. É decisão, não limitação.

**Três escolhas que vale registrar:**

- **O aviso vive acima da hotbar**, na sobreposição do vanilla, e não numa camada de
  interface nossa. O aparelho toca **no bolso**: quem é chamado precisa saber sem estar
  com tela nenhuma aberta. Esse lugar já existe no jogo, e sai sem mixin nem gancho de
  desenho. Ele **desaparece sozinho** depois de alguns segundos, e por isso é reenviado a
  cada segundo enquanto a ligação durar.
- **A lista de quem chamar sai do próprio cliente** — é a mesma que a tecla Tab mostra, já
  sincronizada pelo jogo. Não há pedido ao servidor e não há tela vazia esperando
  resposta, ao contrário da aba social. Ligar manda o **apelido**; o servidor resolve e
  autoriza.
- **Abrir o aparelho tocando cai direto na ligação**, não no menu. Um clique a mais para
  atender é um clique com alguém esperando do outro lado.

⚠️ **O mapa do `CallService` é `ConcurrentHashMap` por necessidade, não por precaução.**
`peerOf` é chamado pela **thread de áudio do SVC**, que não é a do servidor. Um `HashMap`
comum lido de duas threads não devolve só valor velho — pode entrar em laço infinito.

**Nada disso foi visto em jogo, e desta vez nem compilado.** A sessão que escreveu isto
rodou num ambiente cuja política de rede bloqueia `maven.fabricmc.net` (403 no CONNECT),
então o Loom não resolve e `gradlew build` não sai do lugar. Foi conferido o que dava:
JSON válido e Java sem erro de sintaxe (`javac` sem classpath, filtrando o que é
dependência ausente).

⚠️ **Os nomes da API do SVC não passaram pelo `javap`** — o jar da API também não é
alcançável de lá, e a regra da casa é ler os membros reais em vez de tentar variações.
São **quatro chamadas**, todas no mesmo método, e é de propósito que estejam concentradas:
`event.getSenderConnection()`, `event.getVoicechat()`, `api.getConnectionOf(uuid)` e
`api.sendStaticSoundPacketTo(conexão, event.getPacket().staticSoundPacketBuilder().build())`.
Se alguma não fechar, é um arquivo só para consertar.

**Próxima ação:** `gradlew runClient` **nesta branch**, com duas instâncias, e conferir:

1. O log traz `Simple Voice Chat encontrado; plugin do Pokébook registrado.`
2. O botão **Ligações** aparece no menu do aparelho (e **não** aparece sem o SVC).
3. Ligar de um para o outro: toca, o aviso aparece acima da hotbar dos dois, e o sino
   soa só para quem é chamado.
4. Atender **faz o áudio atravessar** — é o ponto que prova a frente inteira.
5. Desligar, recusar e não atender por meio minuto, cada um com a mensagem certa do
   outro lado.
6. Desconectar no meio da ligação não deixa o outro falando com um fantasma.

**Testado em jogo, com dois clients** (ver `TESTES.md` para o como) — **checklist
inteiro confirmado**: toca, atende, o áudio atravessa pelo SVC, desligar e recusar dão a
mensagem certa dos dois lados, não atender por meio minuto desiste sozinho, e desconectar
no meio da ligação desliga em vez de deixar alguém falando com um fantasma.

Um bug apareceu no teste: o **bipe de tocar não soava para quem estava sendo chamado**,
só para quem estivesse perto dele no mundo. Causa: `PokebookSounds.playTo` usava
`PlayerEntity#playSound(SoundEvent, float, float)`, que tem um comportamento não óbvio —
toca posicionado na entidade e **exclui o próprio jogador** de ouvir, porque é pensado
para sons que o cliente já reproduz sozinho (passos, por exemplo), não para avisar
alguém. Trocado por `playSoundToPlayer`, confirmado via `javap` no jar remapeado. Todos
os sons da ligação (toque, tela ligando/desligando) passavam pelo mesmo método, então o
mesmo bug valia para os quatro.

**Adicionado: favoritos na lista de ligar.** Uma estrela (★/☆) por linha, clicável,
independente do resto da linha. Favoritos vão para o topo da lista e continuam
aparecendo mesmo offline, com o rosto escurecido e "Offline" no lugar do "Ligar" — clicar
na linha não manda o pacote de ligar nesse caso, porque o servidor recusaria do mesmo
jeito. É preferência de **cliente**, não de jogador no servidor: guardada num arquivo de
texto na pasta de config (`CallFavorites`), um apelido por linha, comparado ignorando
maiúsculas. Não sincroniza com o servidor de propósito — é a mesma categoria de decisão
que já existia para o filtro de abas da lista de missões, que também é só do cliente.

**Depois, feito nesta rodada:**

- **Mutar na ligação.** Botão novo em `ACTIVE`, ao lado de "Desligar". Servidor é quem
  decide o estado (`CallService.toggleMute`, por participante dentro do `Call`, não um
  mapa à parte) e manda de volta pelo `CallStatePayload`, que ganhou um terceiro campo
  `muted`. `VoicechatIntegration` gira em torno disso: o pacote de microfone continua
  sendo **cancelado** sempre que há ligação atendida (a proximidade continua suprimida),
  mas só é **reenviado** ao par se quem falou não estiver mudo. Mutar não é desligar — a
  ligação continua, só o áudio para de atravessar num sentido.
- **Quem nunca teve poképhone não é mais avisado.** `CallService.hasPhone` confere o
  inventário (`ModItems.POKEPHONE`, não o bloco) antes de mandar o aviso acima da hotbar
  e o toque para quem foi chamado. Sem aparelho, o aviso só confundiria alguém que nunca
  ouviu falar do mod — não há como abrir a tela e atender. A ligação continua "tocando"
  do lado de quem chamou e desiste sozinha depois de meio minuto, igual a ninguém
  atender. Quem não tem celular **continua** na lista de quem chamar — tirar é decisão
  adiada, registrada no `IDEIAS.md`.
- **Grupos do SVC, desabilitados.** `VoicechatIntegration` cancela `CreateGroupEvent` e
  `JoinGroupEvent`. Era a pergunta em aberto do `IDEIAS.md`, e fechou por conferência no
  bytecode do jar do mod (`ServerGroupManager`), não por tentativa: cancelar o evento faz
  o próprio SVC abortar antes de criar o grupo ou confirmar a entrada. Ver a seção
  "Grupos do SVC — resolvido" no `IDEIAS.md` para o detalhe.

**Tudo verificado em jogo:** mutar, favoritos, não avisar quem não tem celular, e os
grupos desabilitados.

### Grupos do SVC: a opção nativa

O autor notou que clicar em "criar grupo" não fazia nada **e não dizia por quê** — o
botão parecia quebrado. Isso levou a uma descoberta que muda a solução:

**O Simple Voice Chat já tem a opção.** `enable_groups=false` no
`config/voicechat/voicechat-server.properties`. Três classes do mod a leem —
`ServerGroupManager` recusa no servidor, `VoiceChatScreen` **esconde o botão** no
cliente, `SecretPacket` sincroniza do servidor para o cliente. Zero código nosso, e é o
caminho suportado, então não quebra em update.

> ⚠️ Isso **corrige** uma afirmação do `IDEIAS.md`: dizia-se que a API do SVC expunha
> acesso à configuração do servidor. Não expõe. O `ConfigAccessor` só tem getters —
> conferido com `javap`. Dá para ler a config, não para mudá-la.

O cancelamento de evento que já existia **fica**, mudando de papel: deixa de ser a
solução e passa a ser a **rede de segurança** para o servidor onde ninguém configurou. E
agora ele **explica ao jogador**, que era o defeito que o autor apontou. A mensagem sai
por `player.server.execute(...)`: o evento chega pela thread do SVC, e mandar pacote de
outra thread é o tipo de coisa que passa no teste e quebra num servidor cheio.

Nota de ambiente: além do `PATH` de terminais antigos, a máquina tem um **JRE 8 da
Oracle** cujo atalho (`C:\Program Files (x86)\Common Files\Oracle\Java\java8path`)
fica no `PATH` do sistema **antes** do Temurin. Se o `JAVA_HOME` não estiver visível
na sessão, o `gradlew` cai nesse Java 8 e falha com *"Gradle requires JVM 17 or
later"*. Terminal novo resolve. Não fixar `org.gradle.java.home` no
`gradle.properties`: é caminho absoluto e o arquivo é versionado, indo para a outra
máquina do autor.

---

### Sessão de 25/09/2026 — os apps baixos e médios do levantamento

Pedido: "todas as baixas e médias, reusando ícones por enquanto". Feito, **na `main`**
(branch de trabalho criada a partir dela) — nada aqui importa Cobblemon nem SVC.

**O que entrou**

| | |
|---|---|
| Fundações | dados do aparelho por jogador (`PhoneData`, um anexo), config do servidor, central de notificações, camada de HUD, missões ao vivo, armazenamento de mundo, tela inicial paginada |
| Contatos | lista no servidor, consentimento para ligar, substitui os favoritos locais da tela de ligações |
| Localização | pontos de interesse com ícone, seta no HUD, local da última morte, mandar no chat, `/pokebook rastrear`, compartilhar com contatos e seguir |
| Missões | acompanhar uma no HUD (◎ na lista) |
| Apps | notas, relógio (hora, dia, lua, previsão, timer, alarme), fotos (câmera e galeria locais), ajustes (não perturbe, quem pode ligar, papel de parede), ranking (só pokébook), radar (só poképhone, só com Cobblemon), avisos |
| Capa | o poképhone entra na tag `#dyeable` e tinge na mesa de trabalho como couro |

**Fora, de propósito:** mensagens, mapa, compartilhar fotos, missões cooperativas (média/alta
ou alta), lanterna (alta sozinha — o caminho barato exige um mod de luz dinâmica, que é
dependência nova e portanto branch nova), e **PC remoto** e **desafio de batalha** — médias,
mas precisam da API do Cobblemon e nasceriam na branch `cobblemon`, que esta sessão não
podia empurrar.

**Decisões que valem registro**

- **Um anexo só, imutável.** `PhoneData` guarda contatos, notas, pontos, ajustes e missão
  acompanhada. Imutável porque a API de anexos só marca sujo ao reatribuir — com registro
  imutável não há como esquecer. Vai inteiro ao cliente a cada mudança, com o mesmo codec
  que grava no disco.
- **O consentimento é sempre de quem recebe.** Salvar alguém não te dá nada sobre ele. Só
  contato liga se *ele* escolheu "só contatos"; só vê onde você está quem *você* marcou.
- **O ranking usa anexo no mundo principal**, não um `PersistentState` à mão — a própria
  Fabric API usa um por baixo (ver `ServerWorldMixin` dela). Mesmo mecanismo das missões.
- **O radar vive na `main`**: reconhece Pokémon pelo id `cobblemon:pokemon` no registro,
  como os sons emprestados. Não mostra shiny nem lendário — isso exigiria a classe dele.
- **O não perturbe é decidido em dois lugares, e de propósito.** No servidor ele recusa
  ligação e suspende localização (regra que outro jogador sente); no cliente ele só cala o
  aviso, que vai para o histórico em silêncio.
- **O botão central sobe até a raiz.** Com telas netas (editar nota, editar ponto), subir
  um degrau faria dele um segundo "voltar" — o próprio comentário do código já avisava.
- **Os ícones são reusados num lugar só**, `AppIcons`. Arte nova para um app é uma linha.

**Os favoritos locais saíram.** A estrela da tela de ligações virou "contato". O arquivo
`config/pokebook-favorites.txt` fica órfão e não é migrado — favorito guardava só o nome, e
contato precisa do UUID, que só se obtém com a pessoa online.

**Nada disso foi compilado nem visto em jogo.** A política de rede desta sessão bloqueava
`maven.fabricmc.net` e o Mojang. O que substituiu o `javap`: os fontes da Fabric API na
versão exata (0.116.17) e os mapeamentos Yarn da 1.21.1 clonados do GitHub, com um script
que decodifica nomes e assinaturas. Todo nome vanilla usado foi conferido assim — com duas
exceções que o script não alcança e ficaram por conhecimento: constantes nomeadas pelo id de
registro (`DataComponentTypes.DYED_COLOR`, `ItemTags.DYEABLE`, `GameRules.DO_WEATHER_CYCLE`)
e componentes de record (`GlobalPos.pos()`, `.dimension()`). Mais: `javac` sem classpath
sem erro de sintaxe, e uma checagem de que toda chamada entre classes do projeto aponta para
membro que existe.

**Próxima ação:** `gradlew build` na `main`. Os erros prováveis, se houver, são de nome —
um por linha, cada um consertável sem mexer no desenho. Depois, `runClient` com duas
instâncias e:

1. Tela inicial: onze apps no poképhone em duas páginas (roda do mouse e pontinhos);
   sete no pokébook, com Ranking e sem Ligações/Avisos/Radar/Fotos/Ajustes.
2. Contatos: salvar alguém online; o outro recebe o aviso. ◎ liga o compartilhamento, e o
   outro vê a pessoa em verde e pode segui-la — a seta anda junto.
3. Ajustes → "só contatos": quem não está salvo recebe "não está recebendo ligações".
   Não perturbe: a ligação é recusada e aparece como perdida em Avisos, sem toast.
4. Pontos: marcar aqui, renomear, trocar ícone, navegar — a seta aponta e some ao chegar.
   Morrer e ver "Última morte" no topo. "No chat" gera texto clicável que liga a seta de
   quem clica.
5. Missões: ◎ numa em andamento, matar o alvo, ver o HUD contar e o toast de concluída.
6. Relógio: hora bate com `/time query daytime`; `/weather rain 2400` (ticks, 2 min) e a
   previsão diz "Chuva · para em ~2 min". Timer de 1 min toca com a tela fechada; alarme toca ao cruzar a hora.
7. Fotos: tirar sai sem HUD e sem o celular; galeria navega e apaga.
8. Capa: poképhone + corante na mesa de trabalho → chassi tingido, tela intacta.
9. `config/pokebook-server.json` com `"radar": false` some com o radar ao reentrar.

### Sessão de 25–26/09/2026 — branch `apps-completo`: as médias da Cobblemon e as difíceis

Pedido: uma branch à parte, **com Cobblemon e voice chat juntos**, com tudo o que já
existia mais o que faltava do levantamento — para o autor revisar e decidir o que entra na
`main`. A branch é `apps-completo`: a `voicechat-nnczo3` (os apps baixos e médios) com
`origin/cobblemon` e `origin/voicechat` mescladas por cima, e o trabalho novo em cima disso.

⚠️ **Esta branch viola a regra de ouro de propósito**: ela junta as duas dependências, e
nada nela deve ser mesclado inteiro na `main`. É vitrine para triagem. O que for aprovado
vai para a `main` **por conteúdo** (ver `CLAUDE.md`, "Conserto de muitos"), deixando para
trás os quatro arquivos do voice chat, o `build.gradle`/`gradle.properties` e a classe de
integração do Cobblemon.

**O que entrou, e para onde iria**

| funcionalidade | aparelho | depende de | iria para |
|---|---|---|---|
| PC remoto | pokébook | Cobblemon (`CobblemonIntegration.openPc`) | pacote e botão na `main`; a abertura do PC na `cobblemon` |
| Desafio de batalha | poképhone (⚔ nos contatos) | Cobblemon (`CobblemonIntegration.startBattle`) | `BattleChallenges` e o comando na `main`; o `Starter` na `cobblemon` |
| Mensagens | poképhone | — | `main` |
| Fotos em mensagem | poképhone | — | `main` |
| Mapa da área | pokébook | — | `main` |
| Lanterna | poképhone | — | `main` |
| Grupos e missões cooperativas | os dois | — | `main` |

A divisão que torna isso possível é a de sempre: `BattleChallenges` não menciona o
Cobblemon — a integração instala um `Starter` ao carregar, e sem ele o botão some e o
servidor recusa. O mesmo vale para o PC: o pacote e o botão são nossos, a abertura é dele.

**Decisões que valem registro**

- **Consentimento para quem está offline: `ContactDirectory`.** Mensagem chega a quem salvou
  quem manda, mas os contatos de um jogador offline estão no save dele. Um espelho só dos
  UUIDs, no mundo principal, atualizado a cada mudança de contatos e corrigido na entrada.
  Divergência cai para o lado seguro: quem não entrou depois da atualização recebe só online.
- **Mensagens num anexo do mundo**, até 100 por conversa. A tela nunca mostra mensagem antes
  de o servidor a gravar. Cada envio vai para o log do servidor — é o registro que o dono de
  servidor público vai querer.
- **Foto em pedaços de 24 KB**, reduzida no cliente (lado de 320, reduzindo mais até caber em
  256 KB). O servidor confere a assinatura PNG, guarda em `<mundo>/pokebook_photos/<uuid>.png`
  e registra no log quem mandou para quem. O id da foto é validado como UUID canônico antes
  de virar caminho — sem isso, `../` num pacote leria qualquer arquivo do servidor.
  **Moderação de imagem não existe**; o que existe é a chave `photo_sharing` e o log.
- **O mapa é do cliente e não pede nada ao servidor.** Mostra o que o cliente já carregou, nas
  cores e no sombreado do mapa do vanilla, 128×128 blocos em volta do pokébook, oito linhas
  por quadro. Reabrir no mesmo lugar em até 30 s reaproveita a imagem.
- **A lanterna é um bloco de luz do vanilla** (`minecraft:light`, nível 13) na cabeça do
  jogador, levado a cada dois ticks e **só em ar** — nunca substitui bloco nenhum, então
  apagar nunca destrói nada. É do servidor: a luz é de verdade e impede monstro de nascer.
  As posições acesas ficam num anexo do mundo; ao ligar o servidor, o que uma queda deixou é
  apagado. O caminho barato de antes (mod de luz dinâmica) continua sendo alternativa, mas
  seria dependência nova.
- **Missão cooperativa: contador no grupo, resgate pessoal.** Ao concluir, cada membro
  ganha a missão como concluída no próprio progresso (`MissionProgress.fill`). Sair do grupo
  depois não tira de ninguém; entrar noutro não deixa resgatar de novo; ranking e aba social
  contam sem saber que grupo existe. Offline na conclusão fica anotado e é entregue na
  entrada. Sem grupo, a missão não anda — e aparece com "· em grupo" no título para dizer
  por quê. A marca vai no título e não num campo novo do pacote: a lista não mudou de forma.
- **Quem sai do grupo não leva o contador**, e um grupo com uma pessoa só deixa de existir.
- **Convites (grupo e batalha) por texto clicável no chat**, com `/pokebook grupo|batalha
  aceitar|recusar <nome>`. O nome serve só para achar o convite; quem autoriza é o registro
  de espera, que só o servidor escreve.

**Nada disso foi compilado nem visto em jogo** — mesma restrição de rede da sessão anterior.
Conferido: nomes Yarn e da Fabric API nos fontes clonados, a API do Cobblemon no fonte da tag
1.8.0 (GitLab), `javac` sem classpath e as checagens de membros, idioma e fronteira.

**Próxima ação:** `gradlew build` na `apps-completo`, em casa. Depois `runClient` com duas
instâncias:

1. Mensagens: salvar um ao outro; mandar texto; sair um dos dois, mandar, entrar — chega o
   aviso "N novas". Sem ser salvo: "essa pessoa não salvou você".
2. Fotos: galeria → Enviar → contato. A conversa mostra "[Foto]"; clicar abre. Conferir
   `<mundo>/pokebook_photos/` e o log.
3. Mapa: pokébook → Mapa. Desenha de cima para baixo; mouse mostra X/Z; clicar num ponto liga
   a seta.
4. Lanterna: à noite, poképhone → Lanterna. Andar, entrar na água (apaga), sair (volta).
   Largar o poképhone no chão apaga. Derrubar o servidor com ela acesa e religar: o log diz
   quantas luzes foram limpas.
5. Grupo: convidar, aceitar pelo chat, matar esqueletos alternando — o contador é um só.
   Aos 20, os dois recebem "seu grupo concluiu" e resgatam cada um no pokébook.
6. Cobblemon: PC no pokébook abre o PC; afastar-se fecha. ⚔ num contato online manda o
   desafio; aceitar começa a batalha 1v1.

---

# Cobblemon — o que aplicar em casa

Estas três mudanças foram escritas, testadas e **revertidas**: a máquina do trabalho
não consegue baixar o jar de 141 MB (ver `CLAUDE.md`, seção do ambiente). Em casa elas
funcionam direto.

**1. `gradle.properties`** — ao final:

```properties
# Cobblemon. O esquema de versão é <versaoCobblemon>+<versaoMinecraft>.
# Fixado de propósito: a API do Cobblemon quebra entre versões menores.
cobblemon_version=1.8.0+1.21.1
```

**2. `build.gradle`** — dentro de `repositories`:

```groovy
// Espelho do ImpactDev, e não o artefacts.cobblemon.com oficial: aquele responde
// 403 (corpo vazio, sem cabeçalho Server) a clientes que não são navegador.
maven {
	name = 'ImpactDev'
	url = uri('https://maven.impactdev.net/repository/development/')
}
```

**3. `build.gradle`** — dentro de `dependencies`:

```groovy
modImplementation "com.cobblemon:fabric:${project.cobblemon_version}"
```

O POM e o repositório já foram validados: o espelho responde 200 e serve o artefato
`1.8.0+1.21.1`. O único obstáculo era o tamanho do jar.

**Ainda não incluído:** o plugin do Kotlin. Para apenas *carregar* o Cobblemon no
ambiente de teste ele não é necessário — o Cobblemon empacota o Fabric Language Kotlin
no próprio jar. Ele passa a ser necessário quando formos **compilar** código Java
contra a API do Cobblemon, para o stdlib do Kotlin resolver no classpath de
desenvolvimento. A MDK oficial usa `kotlin("jvm")` na versão 2.2.20.
