# Wordmed App — Contexto del proyecto

> Documento vivo. Registra lo conversado con Andrés y lo observado en el material.
> Última actualización: 2026-09-08 (sesión de arranque).

---

## 1. Objetivo

Una **app para Android** que:

- Tenga **todas las materias del año** organizadas **en carpetas**.
- Al abrir un tema, lo muestre y lo haga funcionar **exactamente igual** que cuando se
  abre el HTML suelto en el navegador (ejemplo de referencia:
  `Materias/Medicina interna 3/E7. hipertiroidismo.html`).

La app es el **visor**. El contenido lo sigue produciendo el proyecto hermano
`..\Wordmed claude`.

Y al final del proyecto, **una web exactamente igual que la app**, sincronizada con ella.
Ver la sección 3 — no es un extra, condiciona cómo se guarda y se distribuye el contenido.

---

## 2. Requisito duro: fidelidad de render 1:1

Este es el requisito que manda sobre cualquier decisión técnica. Abrir un tema en la app
tiene que dar el **mismo resultado visual y funcional** que abrirlo en el navegador:

- Tipografías, paleta y espaciados del sistema de diseño Wordmed, intactos.
- Los `<script>` inline del cuadernillo tienen que ejecutarse (si no, se rompen las
  interacciones del tema).
- La navegación por **anclas internas** (`#definiciones`, `#fisiopatologia`, `#clinica`,
  `#laboratorio`, …) tiene que seguir funcionando — el índice del cuadernillo depende de eso.
- El comportamiento de tema claro / oscuro que ya trae el HTML.
- **Todo offline.** Los HTML no piden nada a la red; la app tampoco debería.

---

## 3. Segundo destino: la versión web

**Al final del proyecto va a haber también una web, exactamente igual que la app.**
No es un "quizás": está previsto desde ahora y condiciona el diseño.

La regla de oro que pidió Andrés:

> Cada cambio que hagamos — **editar material o agregar material nuevo** — se tiene que
> actualizar **en los dos lados**, como si fuera un pull de GitHub.

Es decir: **un solo origen de verdad para el contenido, dos clientes que lo consumen.**
Nunca copiar temas a mano de un lado al otro; eso se desincroniza el primer día.

### Lo que esto implica

- El contenido (los HTML de los temas) deja de ser "archivos sueltos dentro del APK" y
  pasa a ser un **repositorio de contenido versionado**, consumido por app y web.
- Hace falta un **índice/manifiesto** (por ejemplo `manifest.json`) con las materias, los
  temas, su orden y una versión o hash por tema. Los dos clientes leen el mismo índice.
- La app necesita un **"pull"**: comparar su índice local con el remoto y bajar lo que
  cambió. Offline sigue funcionando con lo último que sincronizó.
- El pipeline de `..\Wordmed claude` pasa a tener un paso más: **publicar** el tema al
  repositorio de contenido (y regenerar el manifiesto), no solo dejarlo en `02_Salida/`.

### DECIDIDO (2026-09-08): mismo contenido, cáscara propia

Andrés eligió la opción **(a)**: la web y la app **comparten el contenido**, pero **cada
una tiene su propia interfaz**. La web no tiene que ser un clon pixel a pixel de la app;
tiene que ofrecer el mismo material y la misma experiencia de lectura.

Esto desbloquea la decisión de stack (sección 6) y define dónde va cada cosa:

| Capa | Quién la escribe | Se comparte |
|---|---|---|
| **Contenido** — HTML de los temas + manifiesto + fuentes + wrapper | el pipeline de `..\Wordmed claude` | **sí, es el contrato** |
| **Cáscara app** — listas de materias/temas, visor | Compose (Kotlin) | no |
| **Cáscara web** — listas de materias/temas, visor | HTML/CSS/JS | no |

### Consecuencia clave: los arreglos del HTML van en la capa de contenido

Los tres problemas de la sección 5 (falta `<meta viewport>`, fuentes de Windows,
`localStorage` con origen `file://`) **le pegan a los dos clientes**, no solo a la app:
un celular abriendo la web tiene exactamente el mismo problema de viewport y de fuentes.

Por eso el **wrapper HTML y las fuentes empaquetadas van en la capa de contenido**, no
dentro de la app. Se arregla una vez y queda arreglado para app y web a la vez. Si lo
resolvemos del lado de Android, hay que volver a resolverlo del lado web.

### DECIDIDO (2026-09-08): distribución por GitHub + host

- **El contenido vive en un repositorio de GitHub.**
- Un host conectado al repo publica la web: cada `push` la redespliega sola
  (GitHub Pages, Cloudflare Pages o Netlify).
- **La app le pega al mismo host**, baja el manifiesto y los temas que cambiaron, y los
  guarda en disco para leer offline.

#### La app NO funciona igual que la web (y a la vez sí)

Andrés supuso que la lógica sería parecida. Lo es **para el contenido**, y no lo es para
la interfaz. La diferencia importa:

| Qué cambiás | Web | App |
|---|---|---|
| **Editar o agregar un tema** | `push` → redeploy → listo | `push` → la app lo ve en el próximo pull. **Sin APK nuevo** |
| **Cambiar la interfaz** (listas, visor) | `push` → listo | **hace falta compilar y reinstalar el APK** |

Un APK instalado no se actualiza solo. Pero como el contenido se lee del host en tiempo de
ejecución y no está horneado adentro del APK, **editar y agregar material —que es el 95 %
de lo que vamos a hacer— sí se comporta igual en los dos lados.** Esa es exactamente la
ventaja de haber puesto el contenido como contrato (decisión (a) de más arriba).

#### Pendiente práctico: repo público o privado

El material deriva de presentaciones de los profesores. Si el repo es público, queda
público en internet. A tener en cuenta al elegir el host:

- **GitHub Pages** desde un repo **privado** requiere plan pago (Pro o superior). En el
  plan gratuito, Pages solo publica desde repos públicos.
- **Cloudflare Pages** y **Netlify** despliegan desde repos privados en su plan gratuito.
- En todos los casos **el sitio publicado es accesible** salvo que se le agregue
  autenticación. Lo que cambia según el host es si el *repositorio* es público.

### DECIDIDO (2026-09-08): estructura y orden del manifiesto

- **Las materias son las carpetas de `Materias/`.** No hace falta declararlas en ningún
  lado: la carpeta es la materia.
- El resto de los metadatos **sí sale de los HTML**, del bloque `<header class="portada">`
  que tienen los 34 temas sin excepción (verificado):

| Dato | De dónde sale |
|---|---|
| Materia | nombre de la carpeta |
| Sub-bloque (Endocrino / Hemato / Reumato…) | `<p class="portada__materia">`, partiendo por ` · ` |
| Nombre del tema | `<title>` (limpio, ej. `Hipertiroidismo`) |
| Bajada / descripción | `<p class="portada__bajada">` — sirve de subtítulo en la lista |
| **Orden** | **el prefijo del nombre de archivo** (`E7.`, `H3`, `2.`, `1.1`) |

**Corrección a lo que se supuso:** el **orden no está escrito en los HTML**. El único
`data-*` que usan es `data-theme`, y la portada no lleva número. El orden vive únicamente
en el **prefijo del nombre de archivo**, que sí es consistente en los 34 temas.

No hace falta tocar los HTML: el generador del manifiesto parsea el prefijo. Tiene que
contemplar los formatos que ya existen: `1.1`, `2.`, `10.`, `E7.`, `H3` (sin punto), `R4`.

Dos detalles menores para el generador:
- `portada__materia` a veces termina en `· Cuadernillo de estudio` y a veces no
  (E7, E8 y H4 no lo tienen). Se recorta ese sufijo.
- **`9. ETS.html` se ignora; el tema 9 de Infecto es `9. ETS v2.html`** (decidido).

#### Recursos: archivos descargables por materia

Además de los temas, una materia puede tener **archivos para descargar** — por ahora
`UM PROGRAMA 2023.pdf` en Medicina Interna III. El manifiesto los lista aparte de los
temas, porque no se leen en el visor: se bajan.

Todo lo que no sea `.html` dentro de la carpeta de una materia entra como recurso.

### DECIDIDO (2026-09-08): el pull lo disparamos nosotros

El `push` al repo se hace **desde acá, cada vez que actualizamos algo**. A partir de ahí
la propagación es automática: el host redespliega la web, y la app levanta el cambio en su
próximo pull. No hay paso manual de copiar temas a ningún lado.

**El repositorio va a ser público.** El host todavía no se eligió; al ser público,
GitHub Pages queda disponible en el plan gratuito y es la opción más directa.

### DECIDIDO (2026-09-08): cuándo y cómo hace el pull la app

**Al abrir la app, y solo si hay una actualización nueva. Si no hay nada nuevo, no hace
nada.**

Para que eso sea posible, cada tema del manifiesto lleva un **hash**: una huella digital
calculada a partir del contenido del archivo. Si el HTML cambia aunque sea una coma, el
hash cambia; si no se toca, queda igual. Hay además un `hash_global` del manifiesto entero.

Secuencia al abrir:

1. La app baja `manifest.json` (unos KB — instantáneo, se hace siempre).
2. Compara el `hash_global` con el que tiene guardado. **¿Igual? Termina acá, cero
   descargas.**
3. ¿Distinto? Compara tema por tema y baja **solo los de hash cambiado**, suma los nuevos
   y borra los que ya no están.

Forma del manifiesto (borrador):

```json
{
  "hash_global": "a3f9c1",
  "materias": [
    {
      "nombre": "Infecto",
      "temas": [
        { "orden": "2", "titulo": "Sepsis y Shock Séptico",
          "archivo": "Infecto/2. Sepsis.html", "hash": "7b2e40" }
      ]
    }
  ]
}
```

**Por qué hash y no fecha ni número de versión a mano:** el hash lo calcula el generador
solo y no depende de que nadie se acuerde de actualizarlo. Una fecha de modificación se
desincroniza sola; un número manual se olvida.

**El `orden` sale del prefijo del nombre de archivo** — confirmado por Andrés. El
generador lo parsea y lo escribe en el manifiesto; los HTML no se tocan.

---

---

## 4. Inventario actual de `Materias/`

| Materia | Temas (.html) | Notas |
|---|---|---|
| `Infecto` | 14 | de `1.1 Toma de muestras y lab` a `13. Fiebre` |
| `Medicina interna 3` | 16 | bloques E1–E8 (endocrino), H1–H4 (hemato), R1–R4 (reumato) + `UM PROGRAMA 2023.pdf` |
| `Psiquiatria` | 4 | de `1. HC y semio psiq` a `4. Esquizofrenia` |
| `Obstetricia` | 0 | **carpeta vacía — falta cargar el material** |

**Pendientes detectados en el material:**

- `Obstetricia/` está vacía.
- En `Infecto/` conviven `9. ETS.html` y `9. ETS v2.html` → hay que definir cuál queda.
- `Medicina interna 3/UM PROGRAMA 2023.pdf` no es un tema: es el programa de la materia.
  Decidir si la app lo ignora, lo muestra aparte, o lo usa como índice de referencia.
- Falta confirmar cuáles son **todas** las materias del año (por ahora hay 4 carpetas).

---

## 5. Características técnicas del material

Verificado sobre `E7. hipertiroidismo.html` (188.680 bytes):

- **No son documentos HTML completos.** No tienen `<!DOCTYPE>`, ni `<html>`, ni `<head>`,
  ni `<body>`. Arrancan directo con `<title>` seguido de un `<style>` inline.
  Son fragmentos con formato de artifact — el navegador los tolera, pero cualquier
  contenedor que los embeba tiene que tenerlo en cuenta.
- **100 % autocontenidos:** CSS inline, 2 `<script>` inline, SVG inline.
  **Cero referencias externas** — no hay ningún `src` o `href` que apunte fuera del
  archivo, solo anclas internas `#seccion`. Funcionan offline tal cual están.
- **Peso típico ~190 KB por tema.** Con ~34 temas actuales el material entero ronda los
  pocos MB; entra sin problema en el APK o en el almacenamiento de la app.
- **El nombre de archivo lleva el prefijo de orden** (`E7.`, `1.1`, `3.`). Sirve para
  ordenar los temas dentro de cada materia sin necesitar metadata aparte.
- El `<title>` del archivo trae el nombre limpio del tema (ej. `Hipertiroidismo`) —
- **No traen `<meta name="viewport">`.** En un navegador de escritorio da igual, pero un
  WebView de Android renderiza con viewport de ~980 px y el tema aparece diminuto y hay
  que zoomear. Hay que envolver el fragmento con un wrapper HTML mínimo (o inyectar el
  meta) antes de mostrarlo en la app. **Aplica a cualquier stack.**
- **No hay `@font-face`: las fuentes son de Windows.** El CSS pide `Sitka Heading`,
  `Segoe UI Variable Text`, `Corbel`, `Cascadia Mono` — ninguna existe en Android, así que
  caen a Roboto/Noto. Para que el render sea de verdad idéntico hay que **empaquetar las
  fuentes en la app** (o aceptar sustitutos y documentarlo). **Aplica a cualquier stack.**
- **Usan `localStorage`** (2 usos) junto con `matchMedia`, `prefers-color-scheme` y
  `data-theme` → el toggle de tema claro/oscuro se persiste. Ojo: con origen `file://` el
  `localStorage` del WebView de Android es poco confiable. Conviene servir los HTML con
  `WebViewAssetLoader` (origen `https://appassets.androidplatform.net/`) en vez de
  `file:///android_asset/`.
  sirve para el listado de la app, mejor que el nombre de archivo.

---

## 6. Stack: **Android nativo (Kotlin) + WebView**

**DECIDIDO (2026-09-08).** Se sigue de dos elecciones de Andrés: pidió instalar las skills
de Kotlin nativo, y eligió la opción (a) de la sección 3 (cáscara propia por cliente), que
es la que mantiene viable el enfoque nativo. Si en algún momento quiere revisarlo, el
análisis completo queda abajo.

- App Android en **Kotlin + Jetpack Compose** para la cáscara (materias → temas).
- Cada tema se abre en un **WebView**, servido con `WebViewAssetLoader` (origen
  `https://appassets.androidplatform.net/`, no `file://`) para que el `localStorage` del
  toggle de tema funcione bien.
- La web se construye aparte, sobre la misma capa de contenido.

### El análisis, para el registro

**A) Android nativo (Kotlin) + WebView por tema** ← elegido
- A favor: fidelidad de render máxima, assets locales, offline real, control total de la
  navegación por carpetas, APK liviano, sin dependencias web.
- En contra: hay que escribir Android; ciclo de build más lento; el HTML se carga en un
  WebView y hay que configurarlo bien (JS habilitado, acceso a archivos locales).

**B) Capacitor / PWA empaquetada**
- A favor: reutiliza lo que Andrés ya sabe de HTML/CSS/JS; un solo código para Android y
  navegador; el visor es literalmente el mismo motor donde ya se ven los cuadernillos.
- En contra: capa extra de tooling (Node, Android Studio igual hace falta para el APK);
  peso del runtime.

**Nota importante:** los dos enfoques renderizan el tema en el **mismo motor** (el WebView
de Android, que es Chromium). La fidelidad de render es idéntica en ambos — **no es un
criterio de decisión**, aunque lo parezca. Capacitor tampoco ahorra el toolchain de
Android: igual hacen falta el SDK y Gradle para generar el APK.

Criterios que definieron la elección: la web lleva cáscara propia (sección 3), así que
Capacitor perdía su ventaja principal — una sola interfaz para los dos destinos. Sumado a
que la cáscara es simple (tres pantallas), que nativo da acceso directo al sistema de
archivos para sincronizar contenido, y que son menos piezas para mantener.

**Costo asumido:** la interfaz se escribe dos veces, una en Compose y otra en web. Es el
precio de la opción (a) y está aceptado.

---

## 7. Relación con `Wordmed claude`

`C:\Users\andre\OneDrive\Documents\Wordmed claude` genera estos HTML:

- Material de entrada (PPTX/PDF de los profesores) en `01_Entrada/`.
- Cuadernillos redactados en `02_Salida/<tema>.html`.
- Pipeline: `node plantilla/construir.mjs 02_Salida/<tema>.html` y
  `python plantilla/exportar_docx.py 02_Salida/<tema>.html`.
- Las reglas clínicas obligatorias, la paleta y la estructura de secciones están en su
  `AGENTS.md`. **Ese archivo es la fuente de verdad del contenido.**

Los HTML de `Materias/` son **copias del producto final**. No se editan acá.
Si un cuadernillo tiene que cambiar, se cambia en el generador y se vuelve a copiar.

Con el requisito de la web (sección 3), el pipeline necesita un **paso de publicación**:
después de construir el tema, publicarlo al repositorio de contenido y regenerar el
manifiesto, para que app y web lo levanten juntas.

---

## 8. Pendientes y decisiones abiertas

> **Andrés va a ir agregando requisitos en esta sección.** Lo de abajo son las preguntas
> que ya quedaron a la vista; no son decisiones tomadas.

**Contenido**
- [x] ~~Confirmar la lista completa de materias del año.~~ **Las 4 que ya están.** Se van
      a ir agregando más si hace falta — decidido 2026-09-08.
- [x] ~~Resolver el duplicado `9. ETS` / `9. ETS v2`.~~ **Se usa `9. ETS v2.html`** —
      decidido 2026-09-08. `9. ETS.html` queda ignorado por el manifiesto.
- [x] ~~Qué hacer con `UM PROGRAMA 2023.pdf`.~~ **Se publica como PDF descargable dentro
      de la carpeta de su materia** — decidido 2026-09-08. Ver "Recursos" abajo.
- [ ] Cargar `Obstetricia/` — Andrés va a subir los HTML.

**Web y sincronización** (ver sección 3)
- [x] ~~Definir qué significa "exactamente igual" para la web.~~ **(a) mismo contenido,
      cáscara propia** — decidido 2026-09-08. Stack: Kotlin nativo (sección 6).
- [x] ~~Dónde vive el repositorio de contenido y cómo se publica.~~ **GitHub + host
      conectado** (Pages / Cloudflare / Netlify) — decidido 2026-09-08.
- [x] ~~Estructura del manifiesto.~~ **Materias = carpetas; metadatos de la portada de
      cada HTML; orden del prefijo del nombre de archivo** — decidido 2026-09-08.
- [x] ~~Quién dispara el pull.~~ **Nosotros, con cada `push`** — decidido 2026-09-08.
- [x] ~~Repo público o privado.~~ **Público** — decidido 2026-09-08.
- [x] ~~Campo de versión por tema.~~ **Hash del contenido, por tema + `hash_global`** —
      decidido 2026-09-08.
- [x] ~~Cuándo hace el pull la app.~~ **Al abrir, y solo si el `hash_global` cambió** —
      decidido 2026-09-08.
- [ ] **Elegir host** (con repo público, GitHub Pages entra en el plan gratuito).
      Postergado a propósito.
- [ ] Qué pasa con un tema que el usuario ya leyó y después cambió.

**Flujo de actualización** — resuelto por las decisiones de la sección 3
- [x] ~~¿Cómo entra un tema nuevo a la app?~~ **La app lo baja del host en el pull al
      abrir. No hace falta APK nuevo** (solo los cambios de interfaz lo requieren).
- [x] ~~¿Hay versionado de temas?~~ **Sí: el hash por tema en el manifiesto.**

**Funcionalidad del visor** — especificada en la sección 10
- [x] ~~Buscador.~~ **Sí, con lupa arriba a la derecha, resultados en vivo y contextual**
      — decidido 2026-09-08.
- [x] ~~Favoritos.~~ **No van.**
- [x] ~~Progreso de lectura.~~ **Barra superior, igual que el HTML** + bloque "Reciente".
- [x] ~~Modo oscuro.~~ **En el cuadernillo, el botón del propio HTML; en la app, menú de
      la esquina superior izquierda.**
- [x] ~~Última posición de lectura.~~ **Sí, se retoma donde quedó.**
- [x] ~~Alcance del buscador.~~ **Títulos + bajada + texto completo, con índice** —
      decidido 2026-09-08.
- [ ] ¿Control de tamaño de fuente? (no mencionado — queda fuera salvo que se pida)
- [ ] ¿Notas o resaltados propios? (no mencionado — queda fuera salvo que se pida)
- [x] ~~Fuentes.~~ **Fuentes libres parecidas, empaquetadas con el contenido** —
      decidido 2026-09-08. Ver sección 9.
- [x] ~~Wrapper con el `<meta viewport>`.~~ **Lo agrega el script de publicación** —
      decidido 2026-09-08. Ver sección 9.
- [x] ~~Cómo se entera de un tema actualizado.~~ **Marca "Actualizado" en la lista hasta
      que lo abre** — decidido 2026-09-08.
- [x] ~~Dónde aparece el PDF del programa.~~ **Botón fijo arriba de todo, en la cabecera
      de la materia** — decidido 2026-09-08. (Nota: funciona bien con uno o dos archivos;
      si más adelante se suman varios PDFs por materia, conviene revisarlo.)

**Distribución** — ver sección 11
- [x] ~~Alcance de la distribución.~~ **Solo Andrés, APK instalado a mano** — decidido
      2026-09-08. Play Store queda como puerta abierta, no como plan.
- [x] ~~Versión mínima de Android.~~ **Android 8 (API 26)** — decidido 2026-09-08.
- [ ] Generar la clave de firma y guardarla fuera del repositorio.

---

## 9. Capa de contenido: envoltorio y fuentes

Las dos cosas que hay que resolver **una sola vez para app y web**, en el script de
publicación al repositorio. Los HTML originales **no se tocan**.

### El envoltorio (`wrapper`)

Los temas son fragmentos sin `<html>`/`<head>`, y sin `<meta name="viewport">` un celular
los renderiza a ~980 px de ancho: el texto sale microscópico.

**No se puede arreglar en el archivo original:** están escritos con el formato que exige
un Artifact, y agregarles `<!DOCTYPE>`/`<head>` los rompería como Artifact. O sirven para
una cosa o para la otra.

Por eso el script de publicación los **envuelve al vuelo**: les antepone el `<!DOCTYPE>`,
el `<head>` con el viewport, el `charset` y el `<link>` a las fuentes, y cierra el
documento. Automático, sin intervención manual.

### Las fuentes

Los cuadernillos piden `Sitka` (títulos), `Segoe UI` (texto) y `Cascadia Mono` (datos).
Las tres vienen con Windows y **ninguna existe en Android**.

Sitka y Segoe UI son de Microsoft y **su licencia no permite redistribuirlas**. Con el
repo público, mandarlas no es opción. Se reemplazan por fuentes libres parecidas, que
viajan con el contenido para que se vea igual en cualquier dispositivo.

#### DECIDIDO (2026-09-08)

| Rol | Original | Reemplazo |
|---|---|---|
| Títulos (`--f-titulo`) | Sitka Heading / Sitka Text | **Spectral** |
| Texto (`--f-texto`) | Segoe UI Variable Text | **Open Sans** |
| Datos (`--f-dato`) | Cascadia Mono | **Cascadia Mono — sin cambio** |

Elegidas por Andrés comparando el cuadernillo real contra el original de Windows:
https://claude.ai/code/artifact/9a4025b1-f055-437b-a88a-4948d56c5395

**Cascadia es software libre** (Microsoft la publicó bajo SIL Open Font License). Se
empaqueta tal cual, así que el monoespaciado queda idéntico al de hoy. Spectral y Open Sans
son de Google Fonts, ambas bajo SIL Open Font License: se pueden redistribuir sin problema
en un repo público.

Stacks para el wrapper:

```css
--f-titulo: "Spectral", Georgia, "Times New Roman", serif;
--f-texto:  "Open Sans", system-ui, -apple-system, sans-serif;
--f-dato:   "Cascadia Mono","Cascadia Code", ui-monospace, Consolas, monospace;
```

Pesos que hay que empaquetar: 400, 600, 700 y la itálica 400 de cada una (el CSS usa 650 y
750; Open Sans es variable y los da exactos, Spectral es estática y redondea a 700).

#### Las fuentes se auto-hospedan, no se enlazan a Google

**No se puede usar `<link>` a `fonts.googleapis.com` en el wrapper.** La app tiene que
funcionar **offline** y la web sin conexión también: si las fuentes vienen de un CDN
externo, el primer día sin internet el cuadernillo se ve con las fuentes del sistema.

Los `.woff2` se guardan **en el repositorio de contenido**, junto a los temas, y el wrapper
los declara con `@font-face` apuntando a rutas locales. Así viajan con el contenido y entran
en el mismo pull que los temas. (En el comparador de arriba sí se usó Google Fonts, porque
es el único host que admite un Artifact — en producción no.)

- [ ] Bajar los `.woff2` de Spectral, Open Sans y Cascadia Mono al repo, y escribir el
      bloque `@font-face` del wrapper.

---

## 10. Funcionalidad del visor y de la app

Especificado por Andrés el 2026-09-08.

### Buscador

- Se abre tocando una **lupa arriba a la derecha**.
- **Resultados en vivo** mientras se escribe, estilo Spotify — no hay botón "buscar".
- **Es contextual:**
  - Abierto **dentro de una materia** → busca solo en esa materia.
  - Abierto **desde afuera** (pantalla de materias) → busca en todas, y **debajo de cada
    resultado, en letra chica, dice de qué materia es**.
- **Dentro de un cuadernillo la lupa desaparece**, para no estorbar la lectura.

### Modo claro / oscuro

Dos lugares distintos según dónde estés:

- **Dentro de un cuadernillo:** en el lugar que ocupaba la lupa va el botón de tema —
  igual que en el HTML.
  **Nota técnica:** los HTML *ya traen* ese botón (`.tema`, fijo arriba a la derecha, con
  su propio `localStorage`). Lo más fiel y lo más simple es **dejar que actúe el del HTML**
  y que la app no dibuje ninguno encima. Conviene además que la app le pase su tema actual
  al abrir el cuadernillo (`data-theme`), para que no se contradigan.
- **En la app, sin cuadernillo abierto:** dentro de un **menú que se despliega desde la
  esquina superior izquierda**.

### Progreso de lectura

- **Sin favoritos ni marcadores.**
- La **barra de progreso va arriba, igual que en el HTML** (el `.progreso` que los
  cuadernillos ya tienen).
- La posición de lectura se guarda **por tema**.

### Bloque "Reciente"

Al volver a abrir la app:

- El **último cuadernillo leído** aparece **arriba de todas las carpetas, solo**.
- Encima lleva el rótulo **"Reciente"**.
- **A la derecha, el progreso** de dónde lo dejó.
- Al tocarlo, **retoma exactamente en la misma parte** donde estaba.

Es uno solo, no una lista de recientes.

### Fuera de alcance por ahora

No se pidieron y no se hacen salvo que Andrés los pida: favoritos, control de tamaño de
fuente, notas o resaltados propios sobre el cuadernillo.

---

## 11. Búsqueda y distribución

### Buscador: títulos + texto completo

DECIDIDO (2026-09-08). El buscador encuentra una palabra **aunque no esté en el título**,
y lleva a la sección donde aparece.

Esto lo hace posible una cosa que los cuadernillos ya traen: **cada `<section class="seccion">`
tiene su propio `id`** (`#laboratorio`, `#fisiopatologia`, …) y su rótulo en
`.seccion__pestana` (`15 Laboratorio`). Verificado: se cumple en los 34 temas, **452
secciones en total**.

Entonces el generador, además del manifiesto, arma un **índice de búsqueda**: por cada
sección guarda su tema, su materia, su `id`, su rótulo y su texto sin etiquetas. Un
resultado puede decir *"Hipertiroidismo · 15 Laboratorio"* y al tocarlo abrir
`E7. hipertiroidismo.html#laboratorio`.

- El índice viaja con el contenido, entra en el mismo pull y funciona offline.
- Peso estimado: unos cientos de KB para los 34 temas actuales.

### Distribución: APK propio

DECIDIDO (2026-09-08). **La app es para Andrés.** Se genera el APK y se instala a mano en
su Galaxy S21 FE (Android 14/15). Sin Play Store, sin trámites, sin costo.

Play Store queda como puerta abierta para más adelante — migrar no pierde nada de lo hecho,
es el mismo proyecto — pero **no es el plan** y no condiciona ninguna decisión de ahora.

**Versión mínima: Android 8 (API 26).** Cubre cualquier teléfono en uso hoy sin arrastrar
código antiguo. No afecta la fidelidad del cuadernillo: el WebView de Android se actualiza
por Play Store aparte del sistema, así que incluso un teléfono viejo lo renderiza bien.

### La clave de firma

Android exige que cada APK esté firmado. La clave se genera **una sola vez** y sirve para
que las versiones nuevas se instalen encima de la vieja **conservando los datos locales**
(progreso de lectura, tema, cuadernillos bajados).

**Si se pierde, no se puede volver a actualizar la app:** habría que desinstalar y
reinstalar de cero, perdiendo el progreso.

- **No va en el repositorio** — es una llave, no contenido. El repo es público.
- Hay que guardarla en un lugar seguro y con respaldo, junto con su contraseña.

### Qué requiere APK nuevo y qué no

| Cambio | ¿APK nuevo? |
|---|---|
| Editar o agregar un cuadernillo | **No** — entra por el pull |
| Agregar una materia | **No** — entra por el pull |
| Cambiar el buscador, las listas, el visor | **Sí** |

---

## 12. Diseño de la interfaz

DECIDIDO (2026-09-08), eligiendo sobre ejemplos con datos reales del manifiesto:
https://claude.ai/code/artifact/3087b482-6fb2-4dc8-a4b5-4e1db3f044d2

**Dirección: Carpetas.** Cada materia es una carpeta con **pestaña de color arriba** — la
misma solapa que usa `.seccion__pestana` en los cuadernillos. La materia se reconoce por
forma y color antes de leer el nombre.

**Lista de temas: versión C3.** Sin descripción, y el número en una **plaquita al costado
del título**, no como pestaña. Entran 10 temas por pantalla contra 3 de la primera versión.

Andrés eligió C3 sabiendo el costo: los temas pierden la forma de carpeta, que queda solo
en la materia y en la cabecera. Fue una decisión informada, no un descuido.

### Lo que quedó fijado

| Pantalla | Cómo es |
|---|---|
| **Materias** | Grilla de 2 columnas de carpetas con pestaña. Arriba, el bloque **Reciente** con el progreso. Obstetricia punteada mientras esté vacía. |
| **Temas** | Cabecera de la materia **sí** con pestaña de carpeta. Temas agrupados por bloque, cada uno con plaquita de número + título. Botón **Programa** solo en materias con PDF. |
| **Buscador** | Abierto desde afuera muestra la materia de cada resultado. Resultados a nivel sección, no solo tema. |

- Paleta: la clínica del cuadernillo. Un color por materia; dentro de Medicina Interna III,
  un color por bloque (Endocrino violeta, Hemato rojo, Reumato amarillo).
- Tipografía: Spectral en títulos, Open Sans en texto, monoespaciada en números.
- Área táctil mínima 44 px en todos los controles.

- [x] Los resultados del buscador **quedan con la pestaña de arriba**, como están.
      Andrés: *"la idea del buscador es encontrar lo que uno busca, no importa el scroll
      porque no va a haber mucho que scrollear en la mayoría de casos"*.

### Ajustes de carpeta (2026-09-08)

- **El título va arriba**, pegado a la pestaña, en todas las carpetas. Las que tienen
  descripción la cuelgan debajo; así todas alinean igual.
- **Infectología es verde.** Cada materia lleva su color y el usuario lo puede cambiar.

## 13. Personalizar carpetas

DECIDIDO (2026-09-08). El usuario puede cambiarle a cada carpeta el **color** y el
**estilo**, y **reordenarlas**.

### El recorrido

1. Menú de la esquina superior izquierda → **"Editar carpetas"**.
2. Entra el modo editar: cada carpeta muestra una **rueda multicolor arriba a la derecha**
   y un **agarre** para arrastrarla a otra posición.
3. La rueda abre un panel con dos solapas: **Colores** y **Estilos**.
   Los colores van en **cuadrícula sin nombres**, separados por grupo con una línea fina.
4. **Botón OK verde abajo a la derecha** confirma todo junto.

### Colores: los siete grupos, completos

**112 colores.** Clínicos · Pasteles · Vivos · Tierra · Joya · Neutros · Neón — 16 cada uno.

### Estilos: los ocho, completos

| | |
|---|---|
| **Original** | Tinte suave, borde fino, pestaña sólida. El del cuadernillo. |
| **Vidrio** | Glassmorphism **teñido con el color de la materia**. |
| **Arcilla** | Redondeado y mullido, relleno saturado. |
| **Futurista** | Fondo negro, borde de neón, monoespaciada. |
| **Papel** | Cartulina torcida, solapa asomando, sombra dura. |
| **Sólido** | La carpeta entera del color, texto blanco. |
| **Contorno** | Solo el borde de color, sin relleno. |
| **Relieve** | Neomorfismo. El color casi no se ve — advertido y aceptado. |

**Regla del vidrio (corregida a pedido de Andrés):** el color se aplica **sobre la carpeta,
nunca sobre el fondo de la app**. El degradé translúcido, el borde de luz y la sombra se
**derivan del color elegido** — verde da un vidrio verde claro con degradé. El fondo de la
pantalla queda siempre igual.

---

## 14. Estado de la construcción (2026-09-08)

### Hecho y verificado en el navegador

| Pieza | Dónde | Estado |
|---|---|---|
| Descarga de fuentes | `publicar/fuentes.mjs` | 22 archivos, 663 KB, licencia libre |
| Envoltorio + manifiesto + índice | `publicar/publicar.mjs` | 33 temas, 429 secciones |
| Puente de lectura | dentro del envoltorio | guarda y retoma la posición |
| Cáscara web completa | `web/` → `contenido/` | navegación, buscador, personalizar, progreso |
| `.gitignore` y `README.md` | raíz | listos |

Probado a 375 px: retoma la lectura en la posición exacta, el buscador contextual filtra por
materia y enlaza a la sección, los 112 colores y los 8 estilos se aplican y persisten.

### Dos hallazgos del camino

- **El nombre real de la materia estaba en los cuadernillos.** El primer segmento de
  `portada__materia` da "Infectología", "Medicina Interna III", "Psiquiatría" — mucho mejor
  que el nombre de la carpeta. El generador ahora lo toma de ahí y usa el más frecuente.
- **`contenido/` es enteramente generado.** La cáscara web vive en `web/` y se copia al
  publicar, así se puede borrar `contenido/` y rehacerla sin perder nada.

### Cómo se guarda todo en el navegador

| Clave | Qué guarda |
|---|---|
| `wordmed-tema` | claro/oscuro — **la misma que usan los cuadernillos**, para que no se contradigan |
| `wordmed:progreso` | por tema: posición, porcentaje y cuándo |
| `wordmed:carpetas` | color, estilo y orden de cada materia |
| `wordmed:hashes` | qué versión de cada tema vimos, para la marca "Actualizado" |

### Lo que falta para el push

- [ ] `git init`, primer commit y push — **esperando el nombre de usuario y del repo**.
- [ ] Conectar Cloudflare Pages con directorio de salida `contenido`.
- [ ] Cargar `Obstetricia/`.
