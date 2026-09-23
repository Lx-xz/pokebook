const fs=require("fs"), zlib=require("zlib");
const A="src/main/resources/assets/pokebook";
function readPNG(file){
  const buf=fs.readFileSync(file); let p=8,w=0,h=0,idat=[];
  while(p<buf.length){const len=buf.readUInt32BE(p),t=buf.toString("ascii",p+4,p+8);
    if(t==="IHDR"){w=buf.readUInt32BE(p+8);h=buf.readUInt32BE(p+12);} if(t==="IDAT")idat.push(buf.subarray(p+8,p+8+len)); p+=12+len;}
  const raw=zlib.inflateSync(Buffer.concat(idat)); const px=Buffer.alloc(w*h*4); let o=0;
  for(let y=0;y<h;y++){const f=raw[o++];const line=raw.subarray(o,o+w*4);o+=w*4;
    for(let x=0;x<w*4;x++){const a=x>=4?px[y*w*4+x-4]:0,b=y>0?px[(y-1)*w*4+x]:0,c=(x>=4&&y>0)?px[(y-1)*w*4+x-4]:0;let v=line[x];
      if(f===1)v+=a;else if(f===2)v+=b;else if(f===3)v+=(a+b)>>1;
      else if(f===4){const pa=Math.abs(b-c),pb=Math.abs(a-c),pc=Math.abs(a+b-2*c);v+=(pa<=pb&&pa<=pc)?a:(pb<=pc?b:c);}
      px[y*w*4+x]=v&255;}}
  return {w,h,px};
}
const model=JSON.parse(fs.readFileSync(A+"/models/block/pokebook.json","utf8"));
const atlas=readPNG(A+"/textures/block/pokebook.png");
const TS=model.texture_size[0];

// "azulidade" media da regiao de UV: identifica a tela sem depender de indice fixo
function azul(uv){
  const x0=Math.floor(Math.min(uv[0],uv[2])/16*TS), x1=Math.ceil(Math.max(uv[0],uv[2])/16*TS);
  const y0=Math.floor(Math.min(uv[1],uv[3])/16*TS), y1=Math.ceil(Math.max(uv[1],uv[3])/16*TS);
  let d=0,n=0;
  for(let y=y0;y<Math.min(y1,atlas.h);y++) for(let x=x0;x<Math.min(x1,atlas.w);x++){
    const i=(y*atlas.w+x)*4; if(atlas.px[i+3]===0)continue; d+=atlas.px[i+2]-atlas.px[i]; n++;
  }
  return n? d/n : 0;
}

let tela=null, maior=-999;
model.elements.forEach((e,ei)=>{ for(const [f,fd] of Object.entries(e.faces)){ const a=azul(fd.uv);
  if(a>maior){maior=a;tela={ei,f};} }});
console.log(`face da TELA detectada: elemento ${tela.ei}, face ${tela.f} (azulidade +${maior.toFixed(1)})`);

let marcadas=0, puladas=[];
model.elements.forEach((e,ei)=>{ for(const [f,fd] of Object.entries(e.faces)){
  const ehTela = (ei===tela.ei && f===tela.f);
  const ehTeclado = (ei===6);           // o teclado tem arte propria, escura
  if(ehTela||ehTeclado){ delete fd.tintindex; puladas.push(`${ei}.${f}`); }
  else { fd.tintindex=0; marcadas++; }
}});
model.__comment = "Geometria exportada do Blockbench. O export vem SEM namespace nas texturas e sem particle - ambos acrescentados aqui. "
 + "texture_size 64 e obrigatorio: as UVs foram escritas para um atlas 64x64 e sem ele saem multiplicadas por 4. "
 + "Os tintindex TAMBEM sao acrescentados aqui: marcam o que recebe a cor do bloco. A face da TELA e as do TECLADO ficam de fora de proposito - "
 + "sem isso um pokebook vermelho ganharia tela vermelha. Um reexport apaga tudo isso; rode o script que redetecta a face da tela pela azulidade da UV em vez de refazer a mao.";
fs.writeFileSync(A+"/models/block/pokebook.json", JSON.stringify(model,null,"\t")+"\n");
console.log(`faces com tintindex: ${marcadas}   sem tint: ${puladas.join(", ")}`);
