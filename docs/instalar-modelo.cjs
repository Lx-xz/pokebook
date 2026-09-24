/**
 * Instala um export do Blockbench como modelo de bloco do pokébook.
 *
 *   node docs/instalar-modelo.cjs docs/modelos/pokebook-v3/pokebook.json
 *
 * O export do Blockbench chega sempre incompleto para uso em mod. Este script repõe
 * tudo o que ele apaga, de forma que reexportar deixe de ser um ritual de memória:
 *
 *   1. NAMESPACE nas texturas e o `particle`. Sem eles o jogo procura em `minecraft:`
 *      e o bloco sai rosa-e-preto; sem particle, as partículas de quebrar saem erradas.
 *
 *   2. Remove as faces com `#missing`. O Blockbench escreve isso nas faces que ficaram
 *      sem textura — as internas dos bisels, as de área zero do touchpad. São invisíveis,
 *      mas geram aviso no log e quads que o jogo desenha à toa.
 *
 *   3. `tintindex` nas faces de chassi, que é o que permite as cores por tingimento.
 *      Ficam de fora a tela, o teclado e o touchpad: a tela porque tem textura e estado
 *      próprios, os outros dois porque tingidos perdem legibilidade — um touchpad
 *      colorido esconde os dois botões.
 *
 *   4. Afasta o touchpad da superfície da base. Ele é um plano de altura zero, e
 *      coplanar com o topo da base dá z-fighting — as duas faces disputam a mesma
 *      profundidade e a imagem pisca.
 *
 *      Apagar os pixels da base sob o touchpad NÃO resolve: o bloco está na camada
 *      `solid`, cujo shader não faz teste de alfa, então a face continua escrevendo
 *      profundidade. Na camada `cutout` resolveria, mas mudar a camada do bloco inteiro
 *      por causa de 8 pixels não se paga. O deslocamento funciona em qualquer camada.
 *
 * Gera também o modelo filho de tela acesa. Depois de rodar, confira o resultado com
 * o validador de assets e rode `gradlew build`.
 */
const fs = require("fs");
const path = require("path");

const ASSETS = "src/main/resources/assets/pokebook";
const NS = "pokebook:block/";

// --- o que o modelo tem de conter. Falhar aqui é melhor que gerar coisa errada em
// --- silêncio: se o Blockbench renumerar os slots, os índices abaixo deixam de valer.
const SLOT_TELA = "2";
const SLOT_COMPONENTES = "3";
const ELEMENTO_TOUCHPAD = 8;
const ELEMENTO_TECLADO = 6;
const FOLGA_TOUCHPAD = 0.01;

const origem = process.argv[2] || "docs/modelos/pokebook-v3/pokebook.json";
if (!fs.existsSync(origem)) {
	console.error("não achei o export: " + origem);
	process.exit(1);
}

const m = JSON.parse(fs.readFileSync(origem, "utf8"));
delete m.credit;

// ---- verificações antes de mexer em qualquer coisa
const problemas = [];
if (!m.texture_size) problemas.push("sem texture_size — as UVs sairiam multiplicadas");
if (!(SLOT_TELA in m.textures)) problemas.push(`slot ${SLOT_TELA} (tela) não existe`);
if (!(SLOT_COMPONENTES in m.textures)) problemas.push(`slot ${SLOT_COMPONENTES} (componentes) não existe`);
if (!m.elements[ELEMENTO_TOUCHPAD]) problemas.push(`elemento ${ELEMENTO_TOUCHPAD} (touchpad) não existe`);
if (!m.elements[ELEMENTO_TECLADO]) problemas.push(`elemento ${ELEMENTO_TECLADO} (teclado) não existe`);

const tp = m.elements[ELEMENTO_TOUCHPAD];
if (tp && tp.from[1] !== tp.to[1]) {
	problemas.push(`elemento ${ELEMENTO_TOUCHPAD} não é um plano (altura ${tp.to[1] - tp.from[1]}) — confira se ainda é o touchpad`);
}
const temTela = m.elements.some(e => Object.values(e.faces).some(f => f.texture === "#" + SLOT_TELA));
if (!temTela) problemas.push(`nenhuma face usa o slot ${SLOT_TELA} — a tela mudou de slot?`);

if (problemas.length) {
	console.error("O export não tem a forma esperada. Os índices deste script viraram mentira:\n");
	for (const p of problemas) console.error("  - " + p);
	console.error("\nAbra o modelo, veja o que mudou, e ajuste as constantes no topo do script.");
	process.exit(1);
}

// ---- 2. faces sem textura
let removidas = 0;
for (const e of m.elements) {
	for (const [face, d] of Object.entries(e.faces)) {
		if (d.texture === "#missing") {
			delete e.faces[face];
			removidas++;
		}
	}
}

// ---- 4. touchpad fora do plano da base
const baseTopo = Math.max(...m.elements.map(e => e.to[1]).filter((_, i) => i === 0));
const alvo = baseTopo + FOLGA_TOUCHPAD;
tp.from[1] = alvo;
tp.to[1] = alvo;

// ---- 3. tintindex
let tingidas = 0;
const livres = [];
m.elements.forEach((e, i) => {
	for (const [face, d] of Object.entries(e.faces)) {
		const fora = d.texture === "#" + SLOT_TELA || i === ELEMENTO_TOUCHPAD || i === ELEMENTO_TECLADO;
		if (fora) {
			delete d.tintindex;
			livres.push(`${i}.${face}`);
		} else {
			d.tintindex = 0;
			tingidas++;
		}
	}
});

// ---- 1. namespace e particle
m.textures = {
	[SLOT_TELA]: NS + "screen_off",
	[SLOT_COMPONENTES]: NS + "components",
	particle: NS + "components",
};

m.__comment = "GERADO por docs/instalar-modelo.cjs a partir de " + path.basename(origem)
	+ ". Não edite à mão: rode o script de novo depois de reexportar do Blockbench. "
	+ "Ele repõe o namespace e o particle, remove as faces #missing do export, aplica os tintindex "
	+ "de chassi (deixando fora tela, teclado e touchpad) e afasta o touchpad da base para não dar z-fighting.";

fs.writeFileSync(ASSETS + "/models/block/pokebook.json", JSON.stringify(m, null, "\t") + "\n");

fs.writeFileSync(ASSETS + "/models/block/pokebook_on.json", JSON.stringify({
	__comment: "GERADO por docs/instalar-modelo.cjs. Só troca a textura da tela; a geometria vem do pai. "
		+ "O brilho emissivo não é declarado aqui — o EmissiveScreenModel seleciona por SPRITE, e block/screen_on já está na lista dele.",
	parent: NS + "pokebook",
	textures: { [SLOT_TELA]: NS + "screen_on" },
}, null, "\t") + "\n");

console.log("instalado de " + origem);
console.log("  faces #missing removidas : " + removidas);
console.log("  touchpad                 : y = " + alvo + " (topo da base " + baseTopo + " + " + FOLGA_TOUCHPAD + ")");
console.log("  faces com tintindex      : " + tingidas);
console.log("  faces sem tint           : " + livres.join(", "));
