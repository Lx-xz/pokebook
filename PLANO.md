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

**Cobblemon — na branch `cobblemon`, não na `main`.** As mudanças de build foram
aplicadas, o jar de 141 MB baixou e o jogo sobe com ele. Mas elas **não** entram na
`main`, e a razão é prática: com o Cobblemon na `main`, a máquina do trabalho não
conseguiria nem buildar o projeto — o FortiGate trava no jar (ver `CLAUDE.md`). A
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

**Próxima ação**, na ordem imposta pelas duas máquinas — o que toca o Cobblemon só
acontece em casa, o resto acontece em qualquer lugar:

1. **Redesenho da interface** (pode ser no trabalho, na `main`) — o autor mandou um conceito com cara de macOS: barra de
   título com ← e ✕, botões de ícone, linhas de missão como cartões arredondados, abas
   "novas/completas". Ver `IDEIAS.md`. Decisão pendente: as abas mudam o modelo da tela,
   não só o desenho — filtro no cliente ou duas listas do servidor?
2. **Integração com o Cobblemon** (só em casa, na branch `cobblemon`) —
   `POKEMON_CAPTURED`. Traz o `ObjectiveType.CAPTURE`, que é o segundo valor do enum, e
   um alvo por espécie, que é o **segundo caso concreto** de `TargetMatcher` — é aí que
   o codec de alvo vira despachado por `"type"`, como previsto. A classe que toca o
   Cobblemon fica isolada, pelo mesmo padrão de fronteira usado para cliente/servidor.
3. **Aba social, degrau 1** — ler o progresso dos outros; o dado já é persistido.

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

Depois: decidir como desabilitar os grupos do SVC, que continua sendo a pergunta em
aberto no `IDEIAS.md` — sem isso o canal não é exclusivo, e o celular volta a ser enfeite.

Nota de ambiente: além do `PATH` de terminais antigos, a máquina tem um **JRE 8 da
Oracle** cujo atalho (`C:\Program Files (x86)\Common Files\Oracle\Java\java8path`)
fica no `PATH` do sistema **antes** do Temurin. Se o `JAVA_HOME` não estiver visível
na sessão, o `gradlew` cai nesse Java 8 e falha com *"Gradle requires JVM 17 or
later"*. Terminal novo resolve. Não fixar `org.gradle.java.home` no
`gradle.properties`: é caminho absoluto e o arquivo é versionado, indo para a outra
máquina do autor.

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
