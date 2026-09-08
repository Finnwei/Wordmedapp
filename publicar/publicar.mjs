/* ============================================================
   Wordmed App · Publicación de la capa de contenido
   ------------------------------------------------------------
   Lee Materias/ y escribe contenido/, que es lo que va al
   repositorio de GitHub y consumen por igual la app y la web.

     Materias/<Materia>/<prefijo>. <tema>.html
       → contenido/temas/<materia>/<tema>.html   (envuelto)
     Materias/<Materia>/<otro>.pdf
       → contenido/recursos/<materia>/<archivo>
       → contenido/manifest.json
       → contenido/buscador.json

   Los HTML originales NO se tocan: son fragmentos con formato de
   Artifact y agregarles <head> los rompería como Artifact. El
   envoltorio se aplica acá, al publicar.

     node publicar/publicar.mjs
   ============================================================ */

import { readdir, readFile, writeFile, mkdir, copyFile, rm, stat } from "node:fs/promises";
import { existsSync } from "node:fs";
import { join, extname, basename, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { createHash } from "node:crypto";

const RAIZ = fileURLToPath(new URL("..", import.meta.url));
const ORIGEN = join(RAIZ, "Materias");
const DESTINO = join(RAIZ, "contenido");

/* Temas que existen en la carpeta pero no se publican. */
const IGNORAR = new Set(["Infecto/9. ETS.html"]);

/* Sufijo que a veces trae la portada y no aporta nada. */
const SUFIJO_PORTADA = /·\s*Cuadernillo de estudio\s*$/i;

/* ---------- utilidades ---------- */

const hash = (dato) => createHash("sha256").update(dato).digest("hex").slice(0, 12);

function slug(texto) {
  return texto
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

/* El orden vive en el prefijo del nombre de archivo: "E7.", "H3",
   "1.1", "10.". Devuelve algo ordenable: primero la letra de bloque
   (vacía antes que A-Z), después los números. */
function leerPrefijo(base) {
  const m = /^([A-Za-z]?)\s*([0-9]+(?:\.[0-9]+)*)/.exec(base.trim());
  if (!m) return { etiqueta: "", letra: "￿", numeros: [Infinity] };
  return {
    etiqueta: (m[1] + m[2]).trim(),
    letra: m[1].toUpperCase() || "",
    numeros: m[2].split(".").map(Number),
  };
}

function compararPrefijos(a, b) {
  if (a.letra !== b.letra) return a.letra < b.letra ? -1 : 1;
  const n = Math.max(a.numeros.length, b.numeros.length);
  for (let i = 0; i < n; i++) {
    const x = a.numeros[i] ?? -1;
    const y = b.numeros[i] ?? -1;
    if (x !== y) return x - y;
  }
  return 0;
}

const ENTIDADES = {
  "&nbsp;": " ", "&amp;": "&", "&lt;": "<", "&gt;": ">",
  "&quot;": '"', "&#39;": "'", "&mdash;": "—", "&ndash;": "–", "&middot;": "·",
};

function aTextoPlano(html) {
  return html
    .replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/&[a-z#0-9]+;/gi, (e) => ENTIDADES[e.toLowerCase()] ?? " ")
    .replace(/\s+/g, " ")
    .trim();
}

/* ---------- lectura del cuadernillo ---------- */

function partir(html) {
  const mTitulo = /<title>([\s\S]*?)<\/title>/i.exec(html);
  const mEstilo = /<style>([\s\S]*?)<\/style>/i.exec(html);
  if (!mTitulo) throw new Error("no tiene <title>");
  if (!mEstilo) throw new Error("no tiene <style>");
  const cuerpo = html.slice(mEstilo.index + mEstilo[0].length).trim();
  return { titulo: mTitulo[1].trim(), estilo: mEstilo[1], cuerpo };
}

function leerPortada(cuerpo) {
  const linea = /<p class="portada__materia">([\s\S]*?)<\/p>/i.exec(cuerpo)?.[1] ?? "";
  const bajada = /<p class="portada__bajada">([\s\S]*?)<\/p>/i.exec(cuerpo)?.[1] ?? "";

  /* "Medicina Interna III · Endocrinología · Cuadernillo de estudio"
     → bloque "Endocrinología". En Infecto no queda nada: no hay bloque. */
  const partes = aTextoPlano(linea)
    .replace(SUFIJO_PORTADA, "")
    .split("·")
    .map((p) => p.trim())
    .filter(Boolean);

  return {
    /* El primer segmento es el nombre real de la materia
       ("Infectología", "Medicina Interna III"), mejor que el de la carpeta. */
    materia: partes[0] || null,
    bloque: partes.length > 1 ? partes.slice(1).join(" · ") : null,
    bajada: aTextoPlano(bajada),
  };
}

/* Recorre las <section class="seccion"> contando anidamiento, por si
   alguna vez llegan a anidarse. Hoy no lo hacen. */
function leerSecciones(cuerpo) {
  const secciones = [];
  const abre = /<section\b[^>]*>/gi;
  let m;

  while ((m = abre.exec(cuerpo)) !== null) {
    if (!/class="[^"]*\bseccion\b/.test(m[0])) continue;

    const id = /id="([^"]+)"/.exec(m[0])?.[1];
    if (!id) continue;

    /* buscar el </section> que cierra a esta */
    let nivel = 1;
    const escaner = /<section\b[^>]*>|<\/section>/gi;
    escaner.lastIndex = abre.lastIndex;
    let fin = -1;
    let s;
    while ((s = escaner.exec(cuerpo)) !== null) {
      nivel += s[0][1] === "/" ? -1 : 1;
      if (nivel === 0) { fin = s.index; break; }
    }
    if (fin === -1) continue;

    const interior = cuerpo.slice(abre.lastIndex, fin);
    const pestana = /<span class="seccion__pestana">([\s\S]*?)<\/span>/i.exec(interior)?.[1] ?? "";
    const titulo = /<h2 class="seccion__titulo">([\s\S]*?)<\/h2>/i.exec(interior)?.[1] ?? "";

    secciones.push({
      id,
      rotulo: aTextoPlano(pestana),
      titulo: aTextoPlano(titulo),
      texto: aTextoPlano(interior.replace(/<span class="seccion__pestana">[\s\S]*?<\/span>/i, "")),
    });
  }
  return secciones;
}

/* ---------- el envoltorio ---------- */

/* Puente entre el cuadernillo y quien lo muestre (web o app):
   guarda dónde quedó la lectura, la retoma al volver, y agrega el
   botón de volver salvo que lo esté abriendo la app —que pone su
   propia flecha— o que se abra suelto en una pestaña. */
const PUENTE = `
(function(){
  "use strict";
  var clave = document.body.dataset.clave || "";
  var enApp = typeof window.WordmedApp !== "undefined";
  var MEM = "wordmed:progreso";

  function leer(){ try{ return JSON.parse(localStorage.getItem(MEM)) || {}; }catch(e){ return {}; } }
  function guardar(m){ try{ localStorage.setItem(MEM, JSON.stringify(m)); }catch(e){} }

  function alto(){
    return Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
  }

  /* retomar donde quedó */
  if (clave) {
    var m = leer()[clave];
    if (m && m.y > 0) {
      window.addEventListener("load", function(){
        window.scrollTo(0, Math.min(m.y, alto()));
      });
    }
  }

  /* anotar el avance, sin ahogar el scroll */
  var pendiente = false;
  window.addEventListener("scroll", function(){
    if (pendiente || !clave) return;
    pendiente = true;
    requestAnimationFrame(function(){
      pendiente = false;
      var y = window.scrollY;
      var pct = Math.round((y / alto()) * 100);
      pct = Math.max(0, Math.min(100, pct));
      var m = leer();
      m[clave] = { y: y, pct: pct, ts: Date.now() };
      guardar(m);
      if (enApp && window.WordmedApp.progreso) window.WordmedApp.progreso(clave, pct, y);
    });
  }, { passive: true });

  /* volver: solo cuando vinimos de la cáscara web */
  if (!enApp && document.referrer && document.referrer.indexOf(location.origin) === 0) {
    var b = document.createElement("button");
    b.className = "volver";
    b.type = "button";
    b.setAttribute("aria-label", "Volver");
    b.innerHTML = '<svg class="volver__icono" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M15 5 8 12l7 7"/></svg>';
    b.addEventListener("click", function(){ history.back(); });
    document.body.appendChild(b);
  }
})();
`;

function envolver({ titulo, estilo, cuerpo, clave }) {
  return `<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${titulo}</title>
<link rel="stylesheet" href="../../fuentes/fuentes.css">
<style>${estilo}</style>
<style>
/* Fuentes de la capa de contenido. Va después del estilo del
   cuadernillo para pisar las de Windows, que no se pueden
   redistribuir y no existen en Android. */
:root{
  --f-titulo:'Spectral',Georgia,'Times New Roman',serif;
  --f-texto:'Open Sans',system-ui,-apple-system,sans-serif;
  --f-dato:'Cascadia Mono','Cascadia Code',ui-monospace,Consolas,monospace;
}
/* Botón de volver, espejo del de tema que ya trae el cuadernillo. */
.volver{
  position:fixed; top:.7rem; left:.7rem; z-index:70;
  width:2.35rem; height:2.35rem; padding:0;
  display:grid; place-items:center;
  cursor:pointer;
  color:var(--tinta-media);
  background:var(--hoja);
  border:1px solid var(--linea-fuerte);
  border-radius:50%;
  box-shadow:var(--sombra);
  transition:color .15s, background .15s, border-color .15s;
}
.volver:hover{ color:var(--cli-t); border-color:var(--cli-c); background:var(--hoja-alt); }
.volver__icono{ width:1.15rem; height:1.15rem; display:block; }
</style>
</head>
<body data-clave="${clave}">
${cuerpo}
<script>${PUENTE}</script>
</body>
</html>
`;
}

/* ---------- publicación ---------- */

/* ---------- red de seguridad ----------

   contenido/ es salida generada: el script la borra y la rehace en cada
   corrida. Si alguien deja ahí un archivo suyo, se pierde. Ya pasó una
   vez con los cuadernillos de Obstetricia.

   Antes de borrar nada, comparamos lo que hay con lo que generamos la
   vez anterior. Cualquier archivo que no reconozcamos frena la corrida. */

async function generadosAntes() {
  const f = join(DESTINO, "manifest.json");
  if (!existsSync(f)) return null;
  try {
    const m = JSON.parse(await readFile(f, "utf8"));
    return new Set(
      m.materias.flatMap((x) => [
        ...x.temas.map((t) => t.archivo),
        ...x.recursos.map((r) => r.archivo),
      ])
    );
  } catch {
    return null;
  }
}

async function buscarAjenos() {
  const conocidos = await generadosAntes();
  if (conocidos === null) return [];

  const ajenos = [];
  for (const sub of ["temas", "recursos"]) {
    const dir = join(DESTINO, sub);
    if (!existsSync(dir)) continue;
    const entradas = await readdir(dir, { recursive: true, withFileTypes: true });
    for (const e of entradas) {
      if (!e.isFile()) continue;
      const abs = join(e.parentPath ?? e.path, e.name);
      const rel = relative(DESTINO, abs).split(sep).join("/");
      if (!conocidos.has(rel)) ajenos.push(rel);
    }
  }
  return ajenos;
}

/* La cáscara web vive en web/ y se copia a contenido/ al publicar,
   para que contenido/ sea enteramente generado y se pueda borrar y
   rehacer sin perder nada. */
async function copiarCascara() {
  const origen = join(RAIZ, "web");
  await mkdir(join(DESTINO, "web"), { recursive: true });
  await copyFile(join(origen, "index.html"), join(DESTINO, "index.html"));
  await copyFile(join(origen, "estilos.css"), join(DESTINO, "web", "estilos.css"));
  await copyFile(join(origen, "app.js"), join(DESTINO, "web", "app.js"));
}

async function main() {
  const ajenos = await buscarAjenos();
  if (ajenos.length > 0 && !process.argv.includes("--forzar")) {
    const aviso = [
      "",
      "  FRENO DE SEGURIDAD",
      "",
      "  Hay archivos en contenido/ que este script no generó:",
      "",
      ...ajenos.slice(0, 20).map((a) => "    " + a),
      ajenos.length > 20 ? "    ...y " + (ajenos.length - 20) + " más" : null,
      "",
      "  contenido/ se borra y se rehace en cada corrida, así que esos",
      "  archivos se perderían.",
      "",
      "  Si son cuadernillos tuyos, movelos a Materias/<Materia>/ y volvé",
      "  a correr el script: de ahí salen publicados solos.",
      "  Si sabés que sobran, corré con --forzar para borrarlos.",
      "",
    ].filter((l) => l !== null);
    for (const l of aviso) console.error(l);
    process.exit(1);
  }

  await rm(join(DESTINO, "temas"), { recursive: true, force: true });
  await rm(join(DESTINO, "recursos"), { recursive: true, force: true });
  await copiarCascara();

  const carpetas = (await readdir(ORIGEN, { withFileTypes: true }))
    .filter((d) => d.isDirectory())
    .map((d) => d.name)
    .sort((a, b) => a.localeCompare(b, "es"));

  const materias = [];
  const entradas = [];
  const avisos = [];

  for (const nombreMateria of carpetas) {
    const materiaSlug = slug(nombreMateria);
    const dirOrigen = join(ORIGEN, nombreMateria);
    const archivos = (await readdir(dirOrigen, { withFileTypes: true }))
      .filter((d) => d.isFile())
      .map((d) => d.name);

    const temas = [];
    const recursos = [];
    const nombresVistos = [];

    for (const archivo of archivos) {
      const relativo = `${nombreMateria}/${archivo}`;
      if (IGNORAR.has(relativo)) {
        avisos.push(`ignorado por configuración: ${relativo}`);
        continue;
      }

      /* --- recursos: todo lo que no sea .html --- */
      if (extname(archivo).toLowerCase() !== ".html") {
        const dir = join(DESTINO, "recursos", materiaSlug);
        await mkdir(dir, { recursive: true });
        const salida = `${slug(basename(archivo, extname(archivo)))}${extname(archivo).toLowerCase()}`;
        await copyFile(join(dirOrigen, archivo), join(dir, salida));
        const datos = await readFile(join(dirOrigen, archivo));
        recursos.push({
          nombre: basename(archivo, extname(archivo)),
          archivo: `recursos/${materiaSlug}/${salida}`,
          tipo: extname(archivo).toLowerCase().slice(1),
          bytes: datos.length,
          hash: hash(datos),
        });
        continue;
      }

      /* --- temas --- */
      const base = basename(archivo, ".html");
      const bruto = await readFile(join(dirOrigen, archivo), "utf8");

      let partes;
      try {
        partes = partir(bruto);
      } catch (e) {
        avisos.push(`SALTEADO ${relativo}: ${e.message}`);
        continue;
      }

      const prefijo = leerPrefijo(base);
      const portada = leerPortada(partes.cuerpo);
      if (portada.materia) nombresVistos.push(portada.materia);
      const secciones = leerSecciones(partes.cuerpo);
      const temaSlug = slug(base);
      const envuelto = envolver({ ...partes, clave: `${materiaSlug}/${temaSlug}` });

      const dir = join(DESTINO, "temas", materiaSlug);
      await mkdir(dir, { recursive: true });
      await writeFile(join(dir, `${temaSlug}.html`), envuelto, "utf8");

      temas.push({
        orden: prefijo.etiqueta,
        _prefijo: prefijo,
        slug: temaSlug,
        titulo: partes.titulo,
        bloque: portada.bloque,
        bajada: portada.bajada,
        archivo: `temas/${materiaSlug}/${temaSlug}.html`,
        secciones: secciones.length,
        bytes: Buffer.byteLength(envuelto),
        hash: hash(envuelto),
      });

      /* el índice de búsqueda: la portada más cada sección */
      entradas.push({
        materia: materiaSlug,
        tema: temaSlug,
        seccion: null,
        rotulo: "Portada",
        titulo: partes.titulo,
        texto: portada.bajada,
      });
      for (const s of secciones) {
        entradas.push({
          materia: materiaSlug,
          tema: temaSlug,
          seccion: s.id,
          rotulo: s.rotulo,
          titulo: s.titulo,
          texto: s.texto,
        });
      }

      if (secciones.length === 0) avisos.push(`sin secciones detectadas: ${relativo}`);
    }

    temas.sort((a, b) => compararPrefijos(a._prefijo, b._prefijo));

    /* colisiones de orden: dos temas con el mismo prefijo */
    const vistos = new Map();
    for (const t of temas) {
      if (t.orden && vistos.has(t.orden)) {
        avisos.push(`orden repetido "${t.orden}" en ${nombreMateria}: ${vistos.get(t.orden)} y ${t.slug}`);
      }
      vistos.set(t.orden, t.slug);
      delete t._prefijo;
    }

    recursos.sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));

    /* nombre de la materia: el que más se repite en las portadas */
    const cuenta = new Map();
    for (const n of nombresVistos) cuenta.set(n, (cuenta.get(n) || 0) + 1);
    const nombre = [...cuenta.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || nombreMateria;

    materias.push({ slug: materiaSlug, nombre, carpeta: nombreMateria, temas, recursos });
    if (temas.length === 0 && recursos.length === 0) {
      avisos.push(`materia vacía: ${nombreMateria}`);
    }
  }

  /* El hash global resume todo: si no cambió, la app no baja nada. */
  const huellas = materias
    .flatMap((m) => [
      ...m.temas.map((t) => `${t.archivo}:${t.hash}`),
      ...m.recursos.map((r) => `${r.archivo}:${r.hash}`),
    ])
    .sort()
    .join("|");

  const manifiesto = {
    generado: new Date().toISOString(),
    hash_global: hash(huellas),
    materias,
  };

  const buscador = { hash_global: manifiesto.hash_global, entradas };

  await writeFile(join(DESTINO, "manifest.json"), JSON.stringify(manifiesto, null, 2), "utf8");
  await writeFile(join(DESTINO, "buscador.json"), JSON.stringify(buscador), "utf8");

  /* ---------- informe ---------- */

  const totalTemas = materias.reduce((n, m) => n + m.temas.length, 0);
  const totalSecciones = materias.reduce((n, m) => n + m.temas.reduce((k, t) => k + t.secciones, 0), 0);
  const totalRecursos = materias.reduce((n, m) => n + m.recursos.length, 0);
  const pesoTemas = materias.reduce((n, m) => n + m.temas.reduce((k, t) => k + t.bytes, 0), 0);
  const pesoBuscador = (await stat(join(DESTINO, "buscador.json"))).size;

  console.log("");
  for (const m of materias) {
    const bloques = [...new Set(m.temas.map((t) => t.bloque).filter(Boolean))];
    console.log(
      `  ${m.nombre.padEnd(22)} ${String(m.temas.length).padStart(2)} temas` +
        (m.recursos.length ? `  ${m.recursos.length} recurso(s)` : "") +
        (bloques.length ? `  [${bloques.join(", ")}]` : "")
    );
  }

  console.log("");
  console.log(`  Temas publicados : ${totalTemas}  (${(pesoTemas / 1048576).toFixed(1)} MB)`);
  console.log(`  Secciones        : ${totalSecciones}`);
  console.log(`  Recursos         : ${totalRecursos}`);
  console.log(`  Índice búsqueda  : ${(pesoBuscador / 1024).toFixed(0)} KB`);
  console.log(`  hash_global      : ${manifiesto.hash_global}`);
  console.log(`  Cáscara web      : index.html + web/`);

  if (avisos.length) {
    console.log("\n  Avisos:");
    for (const a of avisos) console.log(`   · ${a}`);
  }
  console.log("");
}

main().catch((e) => {
  console.error("\nFalló:", e.message);
  console.error(e.stack);
  process.exit(1);
});
