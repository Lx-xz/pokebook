# Pokébook

Um mod de Minecraft que adiciona um notebook funcional — com a intenção de virar um
**sistema de missões** para o [Cobblemon](https://cobblemon.com): receber objetivos
como "capture 3 Pidgeys", acompanhar o progresso e resgatar recompensas.

> 🚧 **Em construção.** No momento existem apenas os assets (modelo e texturas).
> O código ainda não foi escrito.

## Stack

- **Minecraft** 1.21.1
- **Fabric** + Fabric API
- **Java** 21
- **Cobblemon** 1.8.0 (apenas a partir da v2)

## Roadmap

### v1 — o bloco (em andamento)

Fechar o ciclo básico do mod, sem nenhuma dependência externa.

- [x] Modelo 3D no Blockbench, com texturas de tela apagada e acesa
- [x] Blockstate com orientação (`facing`) e estado (`lit`)
- [x] Projeto Gradle e registro do bloco
- [x] Clique com a mão vazia alterna a tela
- [x] Emissão de luz quando aceso
- [x] Aba própria no menu criativo

### v2 — missões

- [ ] Persistência de progresso por jogador
- [ ] Definição de missões via datapack
- [ ] Interface gráfica ao clicar no bloco
- [ ] Integração com o evento `POKEMON_CAPTURED` do Cobblemon
- [ ] Sistema de recompensas

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

- Renderização emissiva (tela brilhando de verdade no escuro)
- Receita de craft
- Som ao ligar/desligar

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
src/main/resources/assets/pokebook/
├── blockstates/pokebook.json      8 variantes (facing × lit)
├── models/
│   ├── block/   pokebook.json · pokebook_on.json
│   └── item/    pokebook.json
├── textures/block/   chassi · keyboard · screen · screen_on · touchpad
└── lang/   en_us.json · pt_br.json
```

## Notas

O modelo foi feito no Blockbench. Ao reexportar, atenção a dois pontos que o
Blockbench não valida: modelos de bloco Java aceitam apenas rotações de `0`, `±22.5`
e `±45` graus, e os caminhos de textura precisam do prefixo de namespace
(`pokebook:block/...`).

Este é um projeto de aprendizado, de uso pessoal. Pokémon e marcas relacionadas
pertencem à Nintendo / Game Freak / The Pokémon Company; este mod não é afiliado a
elas nem ao Cobblemon.
