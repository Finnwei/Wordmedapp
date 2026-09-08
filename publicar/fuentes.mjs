/* ============================================================
   Wordmed App · Descarga de fuentes a la capa de contenido
   ------------------------------------------------------------
   Baja los .woff2 de Spectral, Open Sans y Cascadia Mono a
   contenido/fuentes/ y escribe el fuentes.css con rutas locales.

   Por qué locales y no un <link> a Google: la app tiene que
   funcionar offline. Si las fuentes vienen de un CDN, el primer
   día sin internet el cuadernillo se ve con las del sistema.

   Se corre una sola vez (y de nuevo solo si cambian las fuentes):
     node publicar/fuentes.mjs
   ============================================================ */

import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const RAIZ = fileURLToPath(new URL("..", import.meta.url));
const DESTINO = join(RAIZ, "contenido", "fuentes");

/* Chrome pide woff2; sin este User-Agent, Google devuelve ttf. */
const UA_CHROME =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
  "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

/* Solo estos subconjuntos: alcanzan de sobra para español. */
const SUBCONJUNTOS = ["latin", "latin-ext"];

const GOOGLE =
  "https://fonts.googleapis.com/css2" +
  "?family=Spectral:ital,wght@0,400;0,600;0,700;1,400;1,700" +
  "&family=Open+Sans:ital,wght@0,400;0,600;0,700;1,400;1,700" +
  "&display=swap";

const CASCADIA_BASE =
  "https://cdn.jsdelivr.net/npm/@fontsource/cascadia-mono@5.3.0/files/";

/* Cascadia solo se usa en datos sueltos: números de sección, celdas
   numéricas, contadores. Con regular y negrita alcanza. */
const CASCADIA = [
  { archivo: "cascadia-mono-latin-400-normal.woff2", peso: 400, estilo: "normal" },
  { archivo: "cascadia-mono-latin-700-normal.woff2", peso: 700, estilo: "normal" },
];

async function bajar(url, nombre) {
  const r = await fetch(url, { headers: { "User-Agent": UA_CHROME } });
  if (!r.ok) throw new Error(`${r.status} al bajar ${url}`);
  const datos = Buffer.from(await r.arrayBuffer());
  await writeFile(join(DESTINO, nombre), datos);
  return datos.length;
}

/* Parte la hoja de Google en bloques @font-face y se queda con los
   subconjuntos que nos interesan. Google escribe el nombre del
   subconjunto como comentario justo antes de cada bloque. */
function leerBloques(css) {
  const bloques = [];
  const re = /\/\*\s*([a-z0-9-]+)\s*\*\/\s*(@font-face\s*\{[^}]*\})/gi;
  let m;
  while ((m = re.exec(css)) !== null) {
    const subconjunto = m[1];
    const cuerpo = m[2];
    if (!SUBCONJUNTOS.includes(subconjunto)) continue;

    const familia = /font-family:\s*'([^']+)'/.exec(cuerpo)?.[1];
    const peso = /font-weight:\s*([0-9]+)/.exec(cuerpo)?.[1];
    const estilo = /font-style:\s*([a-z]+)/.exec(cuerpo)?.[1];
    const url = /url\((https:[^)]+\.woff2)\)/.exec(cuerpo)?.[1];
    const rango = /unicode-range:\s*([^;]+);/.exec(cuerpo)?.[1];
    if (!familia || !url) continue;

    bloques.push({ familia, peso, estilo, url, rango, subconjunto });
  }
  return bloques;
}

function nombreLocal(b) {
  const fam = b.familia.toLowerCase().replace(/\s+/g, "-");
  return `${fam}-${b.subconjunto}-${b.peso}-${b.estilo}.woff2`;
}

function reglaFontFace({ familia, peso, estilo, archivo, rango }) {
  return [
    "@font-face{",
    `  font-family:'${familia}';`,
    `  font-style:${estilo};`,
    `  font-weight:${peso};`,
    "  font-display:swap;",
    `  src:url('./${archivo}') format('woff2');`,
    rango ? `  unicode-range:${rango};` : null,
    "}",
  ]
    .filter(Boolean)
    .join("\n");
}

async function main() {
  await mkdir(DESTINO, { recursive: true });

  console.log("Pidiendo la hoja de estilos a Google Fonts…");
  const r = await fetch(GOOGLE, { headers: { "User-Agent": UA_CHROME } });
  if (!r.ok) throw new Error(`Google Fonts respondió ${r.status}`);
  const css = await r.text();

  const bloques = leerBloques(css);
  if (bloques.length === 0) throw new Error("No se reconoció ningún @font-face");

  const reglas = [];
  let total = 0;

  for (const b of bloques) {
    const archivo = nombreLocal(b);
    const bytes = await bajar(b.url, archivo);
    total += bytes;
    console.log(`  ✓ ${archivo}  ${(bytes / 1024).toFixed(1)} KB`);
    reglas.push(reglaFontFace({ ...b, archivo }));
  }

  console.log("Bajando Cascadia Mono…");
  for (const c of CASCADIA) {
    const bytes = await bajar(CASCADIA_BASE + c.archivo, c.archivo);
    total += bytes;
    console.log(`  ✓ ${c.archivo}  ${(bytes / 1024).toFixed(1)} KB`);
    reglas.push(
      reglaFontFace({
        familia: "Cascadia Mono",
        peso: c.peso,
        estilo: c.estilo,
        archivo: c.archivo,
      })
    );
  }

  const cabecera = [
    "/* ============================================================",
    "   Wordmed · fuentes de la capa de contenido",
    "   Generado por publicar/fuentes.mjs — no editar a mano.",
    "",
    "   Spectral y Open Sans: SIL Open Font License 1.1",
    "   Cascadia Mono:        SIL Open Font License 1.1",
    "   Las tres se pueden redistribuir en un repositorio público.",
    "   ============================================================ */",
    "",
  ].join("\n");

  await writeFile(join(DESTINO, "fuentes.css"), cabecera + reglas.join("\n\n") + "\n", "utf8");

  console.log(
    `\nListo: ${reglas.length} archivos, ${(total / 1024).toFixed(0)} KB en total.`
  );
  console.log(`Destino: contenido/fuentes/`);
}

main().catch((e) => {
  console.error("\nFalló:", e.message);
  process.exit(1);
});
