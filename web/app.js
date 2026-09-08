/* ============================================================
   Wordmed · cáscara web
   Lee manifest.json y buscador.json — los mismos archivos que va
   a leer la app — y arma la navegación. Los cuadernillos se abren
   como páginas propias: el visor es el cuadernillo mismo.
   ============================================================ */

"use strict";

/* ---------- paletas ---------- */

const PALETAS = [
  { nombre: "Clínicos", tonos: ["#27AE60","#1B8348","#16A085","#0E7C66","#2980B9","#1F6797","#008CBA","#00708F","#8E44AD","#7D399A","#D4AC0D","#8E7300","#C0392B","#A62F22","#505050","#7C8797"] },
  { nombre: "Pasteles", tonos: ["#A8D5BA","#B8E0D2","#C6E2E9","#A7C7E7","#B5C7ED","#C9B6E4","#E0C3E8","#F7C8D0","#F9C6C6","#F9D5A7","#FDF0B2","#E9EFC0","#D6E5BD","#E8DCC8","#E4C1B9","#D9D9E3"] },
  { nombre: "Vivos", tonos: ["#E74C3C","#FF5722","#E67E22","#FF9800","#F1C40F","#FFC107","#8BC34A","#2ECC71","#1ABC9C","#00BCD4","#3498DB","#2196F3","#673AB7","#9B59B6","#E91E63","#FF4081"] },
  { nombre: "Tierra", tonos: ["#A0522D","#C1440E","#9C6644","#A67B5B","#B7791F","#7D6608","#8B7355","#6E4B3A","#5D4037","#6B8E23","#7F8C4F","#8A9A5B","#B08968","#C9A227","#A68A64","#7C6F57"] },
  { nombre: "Joya", tonos: ["#0F766E","#115E59","#134E4A","#164E63","#1E3A8A","#1E40AF","#312E81","#4C1D95","#581C87","#6B21A8","#3B0764","#831843","#9F1239","#7F1D1D","#92400E","#065F46"] },
  { nombre: "Neutros", tonos: ["#2C3E50","#34495E","#343A40","#495057","#4A4A4A","#5A6472","#6D6875","#6B705C","#737373","#7C8797","#8D99AE","#95A5A6","#A3A3A3","#ADB5BD","#B2BEC3","#C2CBD5"] },
  { nombre: "Neón", tonos: ["#39FF14","#01FF89","#00FFC8","#B6FF00","#CCFF00","#FFFF00","#FFD300","#FF6B00","#FF073A","#FF10F0","#FE01B1","#BC13FE","#7B2FFF","#04D9FF","#00F5FF","#7DF9FF"] },
];

const ESTILOS = [
  { id: "original",  nombre: "Original" },
  { id: "vidrio",    nombre: "Vidrio" },
  { id: "arcilla",   nombre: "Arcilla" },
  { id: "futurista", nombre: "Futurista" },
  { id: "papel",     nombre: "Papel" },
  { id: "solido",    nombre: "Sólido" },
  { id: "contorno",  nombre: "Contorno" },
  { id: "relieve",   nombre: "Relieve" },
];

const COLOR_POR_DEFECTO = {
  "infecto": "#27AE60",
  "medicina-interna-3": "#8E44AD",
  "psiquiatria": "#008CBA",
  "obstetricia": "#D4AC0D",
};
const RUEDA_DE_COLORES = ["#2980B9","#C0392B","#16A085","#D4AC0D","#8E44AD","#008CBA","#E67E22","#505050"];

/* Colores de los bloques dentro de una materia (Endocrino, Hemato, …). */
const COLOR_BLOQUE = ["#8E44AD","#C0392B","#D4AC0D","#2980B9","#27AE60","#008CBA"];

/* ---------- almacenamiento ---------- */

const MEM = {
  carpetas: "wordmed:carpetas",
  progreso: "wordmed:progreso",
  hashes: "wordmed:hashes",
  tema: "wordmed-tema",
};

function leerMem(clave, porDefecto) {
  try { return JSON.parse(localStorage.getItem(clave)) ?? porDefecto; }
  catch (e) { return porDefecto; }
}
function guardarMem(clave, valor) {
  try { localStorage.setItem(clave, JSON.stringify(valor)); } catch (e) {}
}

/* ---------- estado ---------- */

const S = {
  manifiesto: null,
  buscador: null,
  cargandoBuscador: false,
  carpetas: leerMem(MEM.carpetas, {}),
  progreso: leerMem(MEM.progreso, {}),
  hashes: leerMem(MEM.hashes, {}),
  editando: false,
  materiaPanel: null,
  solapaPanel: "colores",
  ruta: { vista: "materias", materia: null },
};

/* ---------- utilidades ---------- */

const $ = (sel) => document.querySelector(sel);

function escapar(t) {
  return String(t).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}

function normalizar(t) {
  return String(t).normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

/* Blanco o tinta oscura según cuánto ilumina el color de fondo. */
function sobre(hex) {
  const n = parseInt(hex.slice(1), 16);
  const [r, g, b] = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => {
    const s = v / 255;
    return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
  });
  const L = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  return L > 0.42 ? "#1A1A1A" : "#FFFFFF";
}

function ajustes(slug, indice) {
  const guardado = S.carpetas[slug] || {};
  return {
    color: guardado.color || COLOR_POR_DEFECTO[slug] || RUEDA_DE_COLORES[indice % RUEDA_DE_COLORES.length],
    estilo: guardado.estilo || "original",
    orden: typeof guardado.orden === "number" ? guardado.orden : indice,
  };
}

function fijarAjuste(slug, campo, valor) {
  S.carpetas[slug] = { ...(S.carpetas[slug] || {}), [campo]: valor };
  guardarMem(MEM.carpetas, S.carpetas);
}

function materiasOrdenadas() {
  return S.manifiesto.materias
    .map((m, i) => ({ ...m, cfg: ajustes(m.slug, i) }))
    .sort((a, b) => a.cfg.orden - b.cfg.orden);
}

function avance(materia, tema) {
  return S.progreso[`${materia}/${tema}`] || null;
}

/* Un tema cambió si su hash difiere del que vimos, y solo importa
   avisarlo si ya lo habíamos empezado a leer. */
function fueActualizado(materia, tema, hash) {
  const clave = `${materia}/${tema}`;
  const visto = S.hashes[clave];
  return Boolean(visto && visto !== hash && S.progreso[clave]);
}

function marcarVisto(materia, tema, hash) {
  S.hashes[`${materia}/${tema}`] = hash;
  guardarMem(MEM.hashes, S.hashes);
}

/* ---------- carga ---------- */

async function cargarManifiesto() {
  const r = await fetch("manifest.json", { cache: "no-cache" });
  if (!r.ok) throw new Error("No se pudo leer el manifiesto");
  S.manifiesto = await r.json();
}

async function cargarBuscador() {
  if (S.buscador || S.cargandoBuscador) return;
  S.cargandoBuscador = true;
  try {
    const r = await fetch("buscador.json", { cache: "no-cache" });
    S.buscador = await r.json();
  } catch (e) {
    S.buscador = { entradas: [] };
  }
  S.cargandoBuscador = false;
}

/* ---------- pintado: materias ---------- */

function pintarMaterias() {
  const vista = $("#vista");
  const ms = materiasOrdenadas();

  const reciente = recienteMasNuevo();
  const partes = [];

  partes.push(`<h1 class="titulo-pantalla">Wordmed</h1>`);

  if (S.editando) {
    partes.push(`
      <div class="aviso-editar">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7.5A1.5 1.5 0 0 1 4.5 6h4L10 8h9.5A1.5 1.5 0 0 1 21 9.5v8a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5z"/><path d="m15.5 11.5 2 2-3.5 3.5h-2v-2z"/></svg>
        <div><b>Editar carpetas</b><span>Tocá la rueda para el color · arrastrá desde el agarre para mover</span></div>
      </div>`);
  } else if (reciente) {
    const m = S.manifiesto.materias.find((x) => x.slug === reciente.materia);
    const t = m && m.temas.find((x) => x.slug === reciente.tema);
    if (m && t) {
      const cfg = ajustes(m.slug, 0);
      partes.push(`
        <div class="reciente">
          <p class="rotulo">Reciente</p>
          <a class="reciente__ficha" href="${escapar(t.archivo)}" style="--c:${cfg.color}"
             data-abrir="${escapar(m.slug)}|${escapar(t.slug)}|${escapar(t.hash)}">
            <span class="carpeta__pestana" style="--c:${cfg.color};--sobre:${sobre(cfg.color)}">Reciente</span>
            <span class="reciente__fila">
              <span style="flex:1;min-width:0;display:flex;flex-direction:column;gap:3px;">
                <span class="reciente__nombre">${escapar(t.titulo)}</span>
                <span class="reciente__donde">${escapar(m.nombre)}${t.bloque ? " · " + escapar(t.bloque) : ""}</span>
              </span>
              <span class="reciente__pct">${reciente.pct}%</span>
            </span>
            <span class="reciente__riel"><span class="reciente__barra" style="width:${reciente.pct}%"></span></span>
          </a>
        </div>`);
    }
  }

  partes.push(`<p class="rotulo">Materias</p>`);
  partes.push(`<div class="rejilla" id="rejilla">${ms.map(carpetaHTML).join("")}</div>`);

  vista.innerHTML = partes.join("");

  if (S.editando) activarEdicion();
  vista.querySelectorAll("[data-abrir]").forEach(recordarAlAbrir);
}

function carpetaHTML(m) {
  const { color, estilo } = m.cfg;
  const vacia = m.temas.length === 0;
  const bloques = [...new Set(m.temas.map((t) => t.bloque).filter(Boolean))];
  const etiqueta = vacia ? "Vacía" : `${m.temas.length} tema${m.temas.length === 1 ? "" : "s"}`;
  const c = vacia ? "var(--linea-fuerte)" : color;

  return `
    <a class="carpeta${vacia ? " carpeta--vacia" : ""}" href="#/m/${escapar(m.slug)}"
       data-slug="${escapar(m.slug)}" data-estilo="${vacia ? "original" : escapar(estilo)}"
       style="--c:${c};--sobre:${vacia ? "#5A6472" : sobre(color)}">
      <span class="carpeta__pestana">${escapar(etiqueta)}</span>
      <span class="carpeta__nombre">${escapar(m.nombre)}</span>
      ${bloques.length ? `<span class="carpeta__desc">${escapar(bloques.join(" · "))}</span>` : ""}
      ${vacia ? `<span class="carpeta__desc">sin material todavía</span>` : ""}
      ${S.editando ? `<button class="rueda" type="button" aria-label="Color y estilo"></button>
        <span class="agarre" aria-hidden="true"><i></i><i></i><i></i></span>` : ""}
    </a>`;
}

function recienteMasNuevo() {
  let mejor = null;
  for (const clave in S.progreso) {
    const v = S.progreso[clave];
    if (!v || !v.ts) continue;
    if (!mejor || v.ts > mejor.ts) {
      const [materia, tema] = clave.split("/");
      mejor = { materia, tema, ts: v.ts, pct: v.pct || 0 };
    }
  }
  return mejor;
}

/* ---------- pintado: una materia ---------- */

function pintarMateria(slug) {
  const vista = $("#vista");
  const idx = S.manifiesto.materias.findIndex((m) => m.slug === slug);
  const m = S.manifiesto.materias[idx];
  if (!m) { location.hash = "#/"; return; }

  const cfg = ajustes(m.slug, idx);
  const bloques = [...new Set(m.temas.map((t) => t.bloque || ""))];
  const partes = [];

  partes.push(`
    <div class="cabecera-materia" style="--c:${cfg.color}">
      <div class="cabecera-materia__texto">
        <span class="cabecera-materia__nombre">${escapar(m.nombre)}</span>
        <span class="cabecera-materia__dato">${m.temas.length} tema${m.temas.length === 1 ? "" : "s"}${bloques.filter(Boolean).length ? ` · ${bloques.filter(Boolean).length} bloques` : ""}</span>
      </div>
      ${m.recursos.map((r) => `
        <a class="boton-recurso" href="${escapar(r.archivo)}" download>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M12 12v6m0 0-2.5-2.5M12 18l2.5-2.5"/></svg>
          ${escapar(r.nombre)}
        </a>`).join("")}
    </div>`);

  if (m.temas.length === 0) {
    partes.push(`<p class="vacio">Todavía no hay cuadernillos en esta materia.</p>`);
  }

  bloques.forEach((bloque, i) => {
    const cb = bloque ? COLOR_BLOQUE[i % COLOR_BLOQUE.length] : cfg.color;
    const temas = m.temas.filter((t) => (t.bloque || "") === bloque);
    if (!temas.length) return;

    if (bloque) {
      partes.push(`
        <div class="bloque" style="--cb:${cb}">
          <span class="bloque__raya"></span>
          <span class="bloque__nombre">${escapar(bloque)}</span>
          <span class="bloque__linea"></span>
        </div>`);
    }

    partes.push(`<div class="temas">${temas.map((t) => {
      const p = avance(m.slug, t.slug);
      const nuevo = fueActualizado(m.slug, t.slug, t.hash);
      return `
        <a class="tema" href="${escapar(t.archivo)}" style="--cb:${cb};--sobre-b:${sobre(cb)}"
           data-abrir="${escapar(m.slug)}|${escapar(t.slug)}|${escapar(t.hash)}">
          <span class="tema__chapa">${escapar(t.orden || "·")}</span>
          <span class="tema__titulo">${escapar(t.titulo)}</span>
          ${nuevo ? `<span class="tema__nuevo">Actualizado</span>` : ""}
          ${p && p.pct > 0 && !nuevo ? `<span class="tema__pct">${p.pct}%</span>` : ""}
        </a>`;
    }).join("")}</div>`);
  });

  vista.innerHTML = partes.join("");
  vista.querySelectorAll("[data-abrir]").forEach(recordarAlAbrir);
}

/* Al abrir un tema anotamos su hash: así deja de figurar como actualizado. */
function recordarAlAbrir(a) {
  a.addEventListener("click", () => {
    const [materia, tema, hash] = a.dataset.abrir.split("|");
    marcarVisto(materia, tema, hash);
  });
}

/* ---------- buscador ---------- */

async function abrirBuscador() {
  const caja = $("#buscador");
  const entrada = $("#buscador-entrada");
  caja.hidden = false;
  document.body.classList.add("sin-scroll");

  const enMateria = S.ruta.vista === "materia" ? S.ruta.materia : null;
  const m = enMateria && S.manifiesto.materias.find((x) => x.slug === enMateria);
  entrada.placeholder = m ? `Buscar en ${m.nombre}` : "Buscar en todas las materias";
  entrada.value = "";
  entrada.focus();

  $("#buscador-resultados").innerHTML = `<p class="cargando">Preparando el índice…</p>`;
  await cargarBuscador();
  $("#buscador-resultados").innerHTML = `<p class="cargando">Escribí para buscar${m ? ` en ${escapar(m.nombre)}` : ""}.</p>`;
}

function cerrarBuscador() {
  $("#buscador").hidden = true;
  document.body.classList.remove("sin-scroll");
}

function buscar(consulta) {
  const caja = $("#buscador-resultados");
  const q = normalizar(consulta.trim());
  const enMateria = S.ruta.vista === "materia" ? S.ruta.materia : null;

  if (q.length < 2) {
    caja.innerHTML = `<p class="cargando">Escribí al menos dos letras.</p>`;
    return;
  }
  if (!S.buscador) { caja.innerHTML = `<p class="cargando">Preparando el índice…</p>`; return; }

  const idxMateria = {};
  S.manifiesto.materias.forEach((m, i) => { idxMateria[m.slug] = { m, cfg: ajustes(m.slug, i) }; });

  const hallazgos = [];
  for (const e of S.buscador.entradas) {
    if (enMateria && e.materia !== enMateria) continue;
    const enTitulo = normalizar(e.titulo).indexOf(q);
    const enTexto = normalizar(e.texto).indexOf(q);
    if (enTitulo === -1 && enTexto === -1) continue;
    hallazgos.push({ e, peso: enTitulo !== -1 ? 0 : 1, pos: enTexto });
    if (hallazgos.length > 400) break;
  }

  if (!hallazgos.length) {
    caja.innerHTML = `<p class="cargando">Nada con “${escapar(consulta)}”${enMateria ? " en esta materia" : ""}.</p>`;
    return;
  }

  hallazgos.sort((a, b) => a.peso - b.peso);
  const temas = new Set(hallazgos.map((h) => h.e.tema));

  const encabezado = `<p class="rotulo">${hallazgos.length} resultado${hallazgos.length === 1 ? "" : "s"} en ${temas.size} tema${temas.size === 1 ? "" : "s"}</p>`;

  const lista = hallazgos.slice(0, 60).map(({ e, pos }) => {
    const ref = idxMateria[e.materia];
    if (!ref) return "";
    const color = ref.cfg.color;
    const tema = ref.m.temas.find((t) => t.slug === e.tema);
    if (!tema) return "";
    const destino = e.seccion ? `${tema.archivo}#${e.seccion}` : tema.archivo;
    const marca = e.seccion ? `${e.rotulo} · ${tema.titulo}` : tema.titulo;

    return `
      <a class="hallazgo" href="${escapar(destino)}" style="--c:${color};--sobre:${sobre(color)}"
         data-abrir="${escapar(e.materia)}|${escapar(e.tema)}|${escapar(tema.hash)}">
        <span class="hallazgo__marca">${escapar(marca)}</span>
        <span class="hallazgo__titulo">${escapar(e.titulo || tema.titulo)}</span>
        <span class="hallazgo__extracto">${extracto(e.texto, q, pos)}</span>
        <span class="hallazgo__materia">${escapar(ref.m.nombre)}</span>
      </a>`;
  }).join("");

  caja.innerHTML = encabezado + lista;
  caja.querySelectorAll("[data-abrir]").forEach(recordarAlAbrir);
}

function extracto(texto, q, pos) {
  if (!texto) return "";
  if (pos === -1 || pos == null) return escapar(texto.slice(0, 150)) + (texto.length > 150 ? "…" : "");
  const desde = Math.max(0, pos - 70);
  const hasta = Math.min(texto.length, pos + q.length + 90);
  const trozo = texto.slice(desde, hasta);
  const rel = pos - desde;
  return (desde > 0 ? "…" : "")
    + escapar(trozo.slice(0, rel))
    + "<b>" + escapar(trozo.slice(rel, rel + q.length)) + "</b>"
    + escapar(trozo.slice(rel + q.length))
    + (hasta < texto.length ? "…" : "");
}

/* ---------- cajón ---------- */

function abrirCajon() {
  $("#cajon").hidden = false;
  $("#velo").hidden = false;
  document.body.classList.add("sin-scroll");
}
function cerrarCajon() {
  $("#cajon").hidden = true;
  $("#velo").hidden = true;
  document.body.classList.remove("sin-scroll");
}

function temaOscuro() {
  const g = localStorage.getItem(MEM.tema);
  if (g === "dark") return true;
  if (g === "light") return false;
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

function alternarTema() {
  const oscuro = !temaOscuro();
  try { localStorage.setItem(MEM.tema, oscuro ? "dark" : "light"); } catch (e) {}
  document.documentElement.setAttribute("data-theme", oscuro ? "dark" : "light");
  $("#interruptor-tema").classList.toggle("encendido", oscuro);
}

/* ---------- modo editar ---------- */

function entrarEnEdicion() {
  S.editando = true;
  document.body.classList.add("editando");
  $("#btn-ok").hidden = false;
  cerrarCajon();
  if (S.ruta.vista !== "materias") location.hash = "#/";
  else pintarMaterias();
}

function salirDeEdicion() {
  S.editando = false;
  document.body.classList.remove("editando");
  $("#btn-ok").hidden = true;
  cerrarPanel();
  pintarMaterias();
}

function activarEdicion() {
  const rejilla = $("#rejilla");
  if (!rejilla) return;

  rejilla.querySelectorAll(".carpeta").forEach((el) => {
    el.addEventListener("click", (ev) => ev.preventDefault());
    const rueda = el.querySelector(".rueda");
    if (rueda) rueda.addEventListener("click", (ev) => {
      ev.preventDefault(); ev.stopPropagation();
      abrirPanel(el.dataset.slug);
    });
    const agarre = el.querySelector(".agarre");
    if (agarre) agarre.addEventListener("pointerdown", (ev) => empezarArrastre(ev, el, rejilla));
  });
}

function empezarArrastre(ev, el, rejilla) {
  ev.preventDefault();
  el.setPointerCapture?.(ev.pointerId);
  el.classList.add("arrastrando");

  const inicio = { x: ev.clientX, y: ev.clientY };
  const mover = (e) => {
    el.style.transform = `translate(${e.clientX - inicio.x}px, ${e.clientY - inicio.y}px) rotate(-1.6deg) scale(1.03)`;
    const hermanos = [...rejilla.querySelectorAll(".carpeta")].filter((x) => x !== el);
    for (const h of hermanos) {
      const r = h.getBoundingClientRect();
      if (e.clientX > r.left && e.clientX < r.right && e.clientY > r.top && e.clientY < r.bottom) {
        const orden = [...rejilla.children];
        if (orden.indexOf(el) < orden.indexOf(h)) h.after(el); else h.before(el);
        inicio.x = e.clientX; inicio.y = e.clientY;
        el.style.transform = "";
        break;
      }
    }
  };
  const soltar = () => {
    el.classList.remove("arrastrando");
    el.style.transform = "";
    document.removeEventListener("pointermove", mover);
    document.removeEventListener("pointerup", soltar);
    [...rejilla.querySelectorAll(".carpeta")].forEach((c, i) => fijarAjuste(c.dataset.slug, "orden", i));
  };
  document.addEventListener("pointermove", mover);
  document.addEventListener("pointerup", soltar);
}

/* ---------- panel ---------- */

function abrirPanel(slug) {
  S.materiaPanel = slug;
  const idx = S.manifiesto.materias.findIndex((m) => m.slug === slug);
  const m = S.manifiesto.materias[idx];
  $("#panel-materia").textContent = m ? m.nombre : "";
  $("#panel").hidden = false;
  $("#velo-panel").hidden = false;
  pintarPanel();
}

function cerrarPanel() {
  $("#panel").hidden = true;
  $("#velo-panel").hidden = true;
  S.materiaPanel = null;
}

function pintarPanel() {
  const slug = S.materiaPanel;
  if (!slug) return;
  const idx = S.manifiesto.materias.findIndex((m) => m.slug === slug);
  const cfg = ajustes(slug, idx);
  const cuerpo = $("#panel-cuerpo");

  $("#panel-valor").textContent = S.solapaPanel === "colores" ? cfg.color.toUpperCase() : "";
  $("#solapa-colores").classList.toggle("activa", S.solapaPanel === "colores");
  $("#solapa-estilos").classList.toggle("activa", S.solapaPanel === "estilos");

  if (S.solapaPanel === "colores") {
    cuerpo.innerHTML = PALETAS.map((g) => `
      <div class="divisor"><b>${escapar(g.nombre)}</b><i></i></div>
      <div class="grilla-tonos">${g.tonos.map((t) => `
        <button class="tono${t.toLowerCase() === cfg.color.toLowerCase() ? " elegido" : ""}" type="button"
                style="background:${t}" data-color="${t}" aria-label="${t}"></button>`).join("")}</div>`).join("");

    cuerpo.querySelectorAll("[data-color]").forEach((b) => {
      b.addEventListener("click", () => {
        fijarAjuste(slug, "color", b.dataset.color);
        pintarMaterias();
        pintarPanel();
      });
    });
  } else {
    cuerpo.innerHTML = `<div class="grilla-estilos">${ESTILOS.map((e) => `
      <button class="muestra${e.id === cfg.estilo ? " elegido" : ""}" type="button" data-estilo="${e.id}">
        <span class="muestra__lienzo">
          <span class="carpeta" data-estilo="${e.id}" style="--c:${cfg.color};--sobre:${sobre(cfg.color)};min-height:0;width:100%;padding:18px 8px 8px;pointer-events:none;">
            <span class="carpeta__pestana" style="font-size:8px;height:15px;padding:0 6px;">Aa</span>
          </span>
        </span>
        <span class="muestra__nombre">${escapar(e.nombre)}</span>
      </button>`).join("")}</div>`;

    cuerpo.querySelectorAll("[data-estilo]").forEach((b) => {
      if (!b.classList.contains("muestra")) return;
      b.addEventListener("click", () => {
        fijarAjuste(slug, "estilo", b.dataset.estilo);
        pintarMaterias();
        pintarPanel();
      });
    });
  }
}

/* ---------- ruteo ---------- */

function rutear() {
  const h = location.hash || "#/";
  const m = /^#\/m\/([^/]+)$/.exec(h);

  if (m) {
    S.ruta = { vista: "materia", materia: decodeURIComponent(m[1]) };
    const mat = S.manifiesto.materias.find((x) => x.slug === S.ruta.materia);
    $("#barra-titulo").textContent = mat ? mat.nombre : "Wordmed";
    $("#btn-menu").innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M15 5 8 12l7 7"/></svg>`;
    $("#btn-menu").setAttribute("aria-label", "Volver");
    pintarMateria(S.ruta.materia);
  } else {
    S.ruta = { vista: "materias", materia: null };
    $("#barra-titulo").textContent = "Wordmed";
    $("#btn-menu").innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h10"/></svg>`;
    $("#btn-menu").setAttribute("aria-label", "Menú");
    pintarMaterias();
  }
  window.scrollTo(0, 0);
}

/* ---------- arranque ---------- */

async function iniciar() {
  try {
    await cargarManifiesto();
  } catch (e) {
    $("#vista").innerHTML = `<p class="vacio">No se pudo cargar el contenido. Probá recargar la página.</p>`;
    return;
  }

  const total = S.manifiesto.materias.reduce((n, m) => n + m.temas.length, 0);
  $("#cajon-resumen").textContent = `${total} temas en ${S.manifiesto.materias.length} materias`;
  $("#cajon-hash").textContent = `Contenido ${S.manifiesto.hash_global}`;
  $("#cajon-fecha").textContent = new Date(S.manifiesto.generado)
    .toLocaleDateString("es-AR", { day: "numeric", month: "long", year: "numeric" });
  $("#interruptor-tema").classList.toggle("encendido", temaOscuro());
  $("#estado-actualizacion").textContent = "al día";

  /* barra superior */
  $("#btn-menu").addEventListener("click", () => {
    if (S.ruta.vista === "materia") history.back();
    else abrirCajon();
  });
  $("#btn-buscar").addEventListener("click", abrirBuscador);
  $("#velo").addEventListener("click", cerrarCajon);
  $("#velo-panel").addEventListener("click", cerrarPanel);

  /* cajón */
  $("#op-tema").addEventListener("click", alternarTema);
  $("#op-editar").addEventListener("click", entrarEnEdicion);
  $("#op-actualizar").addEventListener("click", () => location.reload());

  /* buscador */
  $("#btn-cancelar-buscar").addEventListener("click", cerrarBuscador);
  let temporizador;
  $("#buscador-entrada").addEventListener("input", (e) => {
    clearTimeout(temporizador);
    const v = e.target.value;
    temporizador = setTimeout(() => buscar(v), 110);
  });

  /* panel */
  $("#solapa-colores").addEventListener("click", () => { S.solapaPanel = "colores"; pintarPanel(); });
  $("#solapa-estilos").addEventListener("click", () => { S.solapaPanel = "estilos"; pintarPanel(); });
  $("#btn-ok").addEventListener("click", salirDeEdicion);

  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    if (!$("#buscador").hidden) cerrarBuscador();
    else if (!$("#panel").hidden) cerrarPanel();
    else if (!$("#cajon").hidden) cerrarCajon();
    else if (S.editando) salirDeEdicion();
  });

  window.addEventListener("hashchange", rutear);
  rutear();
}

iniciar();
