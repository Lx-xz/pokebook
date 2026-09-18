# Pokébook

Um mod de Minecraft que adiciona um notebook funcional — com a intenção de virar um
**sistema de missões** para o [Cobblemon](https://cobblemon.com): receber objetivos
como "capture 3 Pidgeys", acompanhar o progresso e resgatar recompensas.

> 🚧 **Em construção.** O bloco, a interface e o sistema de missões funcionam, com
> missões sobre entidades do vanilla **e** sobre Pokémon — capturar e vencer batalhas.
>
> A dependência do Cobblemon vive na branch `cobblemon`, não na `main`. A `main` compila
> e roda sem ele: missões de Pokémon carregam normalmente, apenas nunca progridem. Isso
> é deliberado — permite trabalhar numa rede que não consegue baixar o jar de 141 MB do
> Cobblemon.

## Stack

- **Minecraft** 1.21.1
- **Fabric** + Fabric API
- **Java** 21
- **Cobblemon** 1.8.0 (apenas a partir da v2)

## Roadmap

### v1 — o bloco ✅

Fechar o ciclo básico do mod, sem nenhuma dependência externa.

- [x] Modelo 3D no Blockbench, com texturas de tela apagada e acesa
- [x] Blockstate com orientação (`facing`) e estado da tela (`screen`)
- [x] Projeto Gradle e registro do bloco
- [x] Clique com a mão vazia alterna a tela
- [x] Emissão de luz quando aceso
- [x] Aba própria no menu criativo
- [x] Receita de craft, desbloqueada ao obter um dos ingredientes
- [x] Renderização emissiva (a tela brilha de verdade no escuro)

### v2 — missões

- [x] Interface gráfica ao clicar no bloco, com menu e navegação
- [x] Persistência de progresso por jogador
- [x] Missões com objetivo, alvo, quantidade e recompensa
- [x] Resgate por botão, com recusa quando o inventário está cheio
- [x] Animação de acender e apagar a tela
- [x] Abas por estado e rolagem na lista
- [x] Sons emprestados do Cobblemon
- [x] Definição de missões via datapack
- [x] Integração com o Cobblemon: capturar e vencer batalhas

### v3 — social e comunicação

- [ ] Aba social: ver o progresso de missões dos outros jogadores
- [ ] Poképhone: um irmão portátil do pokébook
- [ ] Mensagens entre jogadores
- [ ] Ligações por voz, integrando o [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat)

A ideia por trás das ligações: com o Simple Voice Chat instalado para voz de
proximidade, o Pokébook desabilitaria o sistema de grupos dele — e a única forma de
conversar à distância passaria a ser ligar pelo celular.

Ver [IDEIAS.md](IDEIAS.md) para o que já foi pesquisado sobre isso.

### Ideias para depois

- Som ao ligar/desligar

## Como funciona

**O bloco é burro de propósito.** Ele não tem `BlockEntity`: o progresso das missões é
**por jogador**, não por bloco, então dois jogadores no mesmo pokébook veem listas
diferentes. O bloco é só um portal, com propriedades de blockstate e nada mais.

**A tela tem quatro níveis** (`screen`, de 0 a 3), não um booleano. Os intermediários
existem para a animação de acender e apagar, feita com ticks agendados de bloco
encadeados — cada tick anda um nível e agenda o próximo. É barato, é salvo junto com o
chunk e sobrevive a recarregar o mundo. A luz acompanha em degraus (`0 / 2 / 5 / 7`),
para o ambiente não piscar antes de a imagem terminar de desbotar.

**As missões são sempre ativas**: não há aceitar nem recusar, o progresso conta
sozinho. São definidas em Java, sobre entidades do **vanilla** — e isso é produto, não
andaime: é o que mantém o sistema verificável sem o Cobblemon instalado, inclusive no
dia em que um update dele quebrar a integração e for preciso saber de quem é o defeito.
O progresso vive na API de anexo do Fabric, com `copyOnDeath()`, e não no
armazenamento do Cobblemon, cuja API quebra entre versões menores.

**O resgate é por botão, não automático.** Com recompensa automática o pokébook
deixaria de ter função — ninguém precisaria abri-lo. Com o inventário cheio o resgate é
recusado e a recompensa continua disponível, em vez de cair aos pés: ela é única, e
item caído sobre lava ou no void some para sempre.

## Missões por datapack

Uma missão é um arquivo JSON em `data/<namespace>/pokebook/mission/<nome>.json`. O id
sai do caminho, como em qualquer pasta de datapack do vanilla: o arquivo
`data/pokebook/pokebook/mission/three_cows.json` vira a missão `pokebook:three_cows`.
As missões que o mod traz usam esse mesmo caminho — não há atalho para as de dentro.

Só `target` é obrigatório. A missão mais simples possível é uma linha:

```json
{ "target": "minecraft:creeper" }
```

Isso já vale: matar um creeper, sem recompensa. Os outros campos têm padrão:

| campo | padrão | o que é |
|---|---|---|
| `target` | — | em quem a missão vale |
| `objective` | `kill` | o que fazer com ele |
| `required` | `1` | quantos |
| `reward` | nenhuma | o item da recompensa |
| `reward_count` | `1` | quantos itens |
| `title` | gerado | o nome na tela |

### Objetivos

| valor | o que conta |
|---|---|
| `kill` | matar uma entidade |
| `capture` | capturar um Pokémon |
| `battle_win` | vencer uma batalha; o alvo é o **Pokémon derrotado** |

`capture` e `battle_win` precisam do Cobblemon para *acontecer*, mas não para
*existir*: uma missão assim carrega numa instalação sem ele e apenas nunca progride.

### Alvos

| forma | exemplo | casa com |
|---|---|---|
| id de entidade | `"minecraft:cow"` | aquele tipo exato de entidade |
| espécie | `{ "species": "pidgey" }` | aquela espécie de Pokémon |
| tipo elemental | `{ "element": "fire" }` | tipo primário **ou** secundário |
| geração | `{ "generation": 1 }` | Pokémon daquela geração |

A forma de objeto é identificada pela **chave que traz** — não há campo `type` de
despacho, para que uma missão de espécie se escreva `{"species": "pidgey"}` e não
`{"type": "pokebook:species", "species": "pidgey"}`.

Declarar duas chaves ao mesmo tempo é **erro**, não precedência silenciosa:
`{"species": "pidgey", "element": "fire"}` quase certamente quer dizer "Pidgey do tipo
fogo", que este modelo não sabe expressar. Falhar alto é melhor do que atender metade
do pedido em silêncio.

Em `species`, o namespace pode ser omitido: `pidgey` vira `cobblemon:pidgey`.
"Geração" é **etiqueta**, não campo — o Cobblemon guarda rótulos como `gen1` na
espécie, e este campo é açúcar sobre isso.

Uma missão completa:

```json
{
	"target": "minecraft:cow",
	"required": 3,
	"reward": "minecraft:leather",
	"reward_count": 8,
	"title": "Abater 3 vacas"
}
```

**Sobre o `title`:** sem ele, o nome é **montado** das peças que a missão já tem —
verbo do objetivo, alvo e quantidade: "Capturar Pidgey ×3". Nenhuma chave de tradução
por missão, porque criar uma missão não pode exigir editar um arquivo de idioma por
língua.

A quantidade vem depois do alvo, com "×", e não "Capturar 3 Pidgeys". Os arquivos de
idioma do Minecraft **não têm plural** e o nome de uma espécie é sempre singular, então
"Capturar 3 Pidgey" sairia errado em qualquer idioma. Com `title`, o texto é usado como
está — útil para a missão que merecer um nome próprio, ao custo de não ser traduzido.

`/reload` recarrega as missões com o mundo aberto, e quem estiver com o pokébook na
tela recebe a lista nova na hora. Um arquivo inválido é registrado no log e pulado:
um erro de digitação numa missão não apaga a lista inteira.

Por enquanto `target` é só o id de um tipo de entidade e `objective` só aceita `kill`.
Quando houver um segundo tipo de alvo — espécie de Pokémon, tipo elemental — o campo
passa a aceitar também um objeto `{"type": ..., ...}`, que é distinguível de uma
string; os datapacks escritos hoje continuam válidos.

## Como rodar

Requer **JDK 21**. O Gradle baixa o Minecraft automaticamente — não é preciso ter o
launcher instalado nem fazer login.

```bash
gradlew runClient
```

A primeira execução leva de 5 a 15 minutos (decompilação e remapeamento do jogo) e
ocupa de 3 a 5 GB. Depois disso, segundos.

## Estrutura

```
src/main/java/io/github/lxxz/pokebook/
├── Pokebook.java              entrypoint comum
├── block/                     o bloco e a animação da tela
├── client/                    entrypoint de cliente
│   └── screen/                menu, lista de missões e a base comum
├── mission/                   missões, progresso, objetivos e alvos
├── network/                   os payloads trocados entre cliente e servidor
├── registry/                  registro de bloco, item e aba do criativo
└── server/                    quem está com o pokébook aberto

src/main/resources/
├── assets/pokebook/
│   ├── blockstates/           16 variantes (facing × screen)
│   ├── models/block/          pokebook · pokebook_1 · pokebook_2 · pokebook_on
│   ├── models/item/           pokebook (com as transformações de exibição)
│   ├── textures/block/        chassi · chassi_back · keyboard · touchpad
│   │                          screen · screen_1 · screen_2 · screen_on
│   ├── textures/gui/          pokebook_gui
│   └── lang/                  en_us · pt_br
└── data/
    ├── pokebook/loot_table/   o bloco dropa a si mesmo
    └── minecraft/tags/block/  quebrável com picareta
```

## Notas

O projeto usa **mappings Yarn**, não as oficiais da Mojang. A documentação atual do
Fabric mostra os nomes da Mojang, que são diferentes (`onUse` e não `useWithoutItem`,
`Identifier` e não `ResourceLocation`) — copiar de lá dá nome inexistente. Quando um
nome não fechar, leia os membros reais do jar remapeado com `javap`.

O modelo foi feito no Blockbench. Ao reexportar, atenção a dois pontos que o
Blockbench não valida: modelos de bloco Java aceitam apenas rotações de `0`, `±22.5`
e `±45` graus, e os caminhos de textura precisam do prefixo de namespace
(`pokebook:block/...`).

Este é um projeto de aprendizado, de uso pessoal. Pokémon e marcas relacionadas
pertencem à Nintendo / Game Freak / The Pokémon Company; este mod não é afiliado a
elas nem ao Cobblemon.
