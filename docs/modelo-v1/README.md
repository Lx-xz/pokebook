# Modelo v1 — arquivo

Os modelos e texturas do pokébook **antes** da remodelagem v2, guardados como
referência. Nada aqui é carregado pelo jogo.

## O que era

- **5 elementos, 30 faces.** Tampa inclinada a **22.5°**, profundidade sugerida só
  pela textura — sem moldura de geometria.
- **Cinco texturas separadas**, uma por slot: chassi, tela, teclado, touchpad e o
  `chassi_back` que chegou a existir e ficou órfão.
- **Quatro níveis de tela** (`screen`, `screen_1`, `screen_2`, `screen_on`), que é o
  que a animação de acender e apagar consome. Os modelos filhos sobrescrevem o slot
  `"1"` e herdam o resto.

## Por que pode ser útil depois

As quatro texturas de tela são uma **rampa de brilho já calibrada** — brilho médio
5.3 → 22.4 → 56.1 → 92.4, medido. Se a v2 precisar dos mesmos quatro níveis, os
valores de interpolação já estão resolvidos aqui.
