# Pokébook — Plano da v1

> **Para retomar numa conversa nova:** leia `CLAUDE.md` primeiro (contexto, decisões
> e armadilhas), depois este arquivo. O ponto exato onde paramos está no final, em
> **Onde paramos**.

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

### 1. Projeto Gradle (raiz)

`build.gradle`, `settings.gradle`, `gradle.properties`, wrapper (`gradlew`,
`gradlew.bat`, `gradle/wrapper/`), `.gitignore`.

Base: template oficial `FabricMC/fabric-example-mod` na branch de 1.21.1.

> ⚠️ **Pendência:** as versões exatas de `yarn_mappings`, `loader_version`,
> `fabric_version` e do plugin `fabric-loom` **ainda não foram levantadas** — a
> consulta à API do Fabric foi interrompida. Buscar em
> `https://meta.fabricmc.net/v2/versions/yarn/1.21.1` e na página do Fabric API no
> Modrinth antes de escrever o `gradle.properties`. **Não chutar esses números.**

### 2. Código Java — `src/main/java/io/github/lxxz/pokebook/`

**`Pokebook.java`** — `ModInitializer`. Constante `MOD_ID = "pokebook"`, um `Logger`,
e `onInitialize()` chamando os registros na ordem certa.

**`block/PokebookBlock.java`** — estende `Block`, concentra o comportamento:

- Propriedades `FACING` (`BlockStateProperties.HORIZONTAL_FACING`) e `LIT`
  (`BlockStateProperties.LIT`), declaradas em `createBlockStateDefinition`
- `getStateForPlacement` → `context.getHorizontalDirection().getOpposite()`, para a
  tela nascer virada ao jogador
- `useWithoutItem(...)` → `level.setBlock(pos, state.cycle(LIT), UPDATE_ALL)`.
  Este método **só é invocado com a mão vazia** — a regra de interação sai de graça,
  sem nenhum `if`. Agir só no servidor (`level.isClientSide`)
- `getShape` → `VoxelShape` própria; o modelo não preenche o cubo, e a colisão padrão
  faria o jogador flutuar sobre o pokébook

**`registry/ModBlocks.java`** — registra bloco e `BlockItem` via
`Registry.register(BuiltInRegistries.BLOCK, ...)`. Propriedades: `strength(1.5f)`,
`sound(SoundType.METAL)`, `noOcclusion()`, `lightLevel(s -> s.getValue(LIT) ? 7 : 0)`.

**`registry/ModItemGroups.java`** — aba própria no criativo via
`FabricItemGroup.builder()`, com o pokébook como ícone. A chave
`itemGroup.pokebook.main` já existe nos arquivos de lang.

### 3. Recursos — `src/main/resources/`

- **`fabric.mod.json`** — id, versão, entrypoint para `Pokebook`, e `depends` em
  `fabricloader`, `minecraft` (`~1.21.1`), `java` (`>=21`), `fabric-api`
- **`data/pokebook/loot_table/blocks/pokebook.json`** — sem isto **o bloco some ao
  ser quebrado**, sem dropar nada
- **`data/minecraft/tags/block/mineable/pickaxe.json`** — quebrável com picareta

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

## Onde paramos

Assets prontos e validados. **Nenhuma linha de Java ou Gradle escrita ainda.**

O plano foi aprovado e a implementação tinha acabado de começar: o primeiro passo era
levantar as versões exatas do Fabric para 1.21.1 — e é exatamente aí que retomamos.

**Próxima ação:** buscar as versões (yarn, loader, fabric-api, loom), escrever o
`gradle.properties` e o `build.gradle`, e seguir para o código Java.

O JDK ainda não está instalado nesta máquina — só há um JRE 8, que não compila. Os
arquivos de texto podem ser escritos sem ele; `gradlew build` e `runClient` só
funcionam depois do JDK 21.
