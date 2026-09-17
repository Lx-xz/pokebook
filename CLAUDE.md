# CLAUDE.md

Contexto para sessões futuras do Claude Code neste projeto.

## O que é

Mod de Minecraft chamado **Pokébook**. Um bloco em formato de notebook cuja tela
acende ao ser clicado.

O destino do projeto **não é decoração**: é um **sistema de missões** ("capture 3
Pidgeys") com recompensas, integrado ao Cobblemon. A v1 atual entrega só o bloco
interativo — é o alicerce, não o objetivo.

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
- **Tela é um interruptor**: acende ao clicar, fica acesa até clicar de novo. Sem tick
  agendado, sem timer.
- **Luz nível 7** quando aceso, 0 quando apagado.
- **Interação só com mão vazia** — sai de graça usando `useWithoutItem`, que o jogo
  só invoca quando a mão principal está vazia. Não escreva um `if` para isso.
- **Template Fabric puro**, sem architectury nem Kotlin. O Cobblemon entra depois
  como repositório Maven + dependência, não exige re-scaffolding.

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

**`noOcclusion()` é obrigatório** — o modelo não preenche o cubo; sem isso as faces
dos blocos vizinhos desaparecem.

**`ResourceLocation` perdeu o construtor público na 1.21** — usar
`ResourceLocation.fromNamespaceAndPath(...)`.

## Sobre o Cobblemon (para quando as missões chegarem)

- Maven: `https://artefacts.cobblemon.com/releases/`
- Coordenadas: `com.cobblemon:fabric:1.8.0+1.21.1`
- MDK oficial com variante **em Java puro**: `gitlab.com/cable-mc/cobblemon-mdks`
- O código-fonte está no **GitLab**, não no GitHub (`Cobblemon/Cobblemon` no GitHub
  não existe, só forks)
- Cobblemon empacota o Fabric Language Kotlin no próprio jar — o usuário final não
  instala nada extra, e um mod em Java puro consome a API normalmente
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

## Estado atual

Assets prontos e validados em `src/main/resources/assets/pokebook/`.
**Nada de Java nem Gradle foi escrito ainda.** Veja `PLANO.md`.
