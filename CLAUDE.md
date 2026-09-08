# Wordmed App — App Android de cuadernillos médicos

App para Android que organiza las materias del año en carpetas y abre cada tema
renderizándolo **exactamente igual** que el HTML suelto que ya genera el proyecto
hermano (`..\Wordmed claude`).

**Antes de tocar cualquier cosa, leer `CONTEXTO.md`.** Ahí está el objetivo, el
inventario de materias, las características técnicas del material y la lista de
decisiones que todavía están abiertas. No improvisar sobre ese contenido.

## Estado actual

Etapa de contexto. **Todavía no hay código de la app.** Lo único que existe es `Materias/`
con el material inicial. Andrés sigue agregando requisitos antes de arrancar el desarrollo.

**Decidido (2026-09-08):**
- **Stack: Android nativo (Kotlin + Compose), con el tema en un WebView.**
- **Habrá también una web**, con cáscara propia pero el **mismo contenido** que la app,
  sincronizado desde un único origen de verdad. Ver `CONTEXTO.md` §3 y §6.

## Skills instaladas en este proyecto

En `.claude/skills/` (alcance proyecto, no global):

| Skill | Fuente | Para qué |
|---|---|---|
| `navigation-3` | Google | Navegación materias → temas, backstack, deep links |
| `adaptive` | Google | UI en Compose que adapta a celular / tablet / plegable |
| `edge-to-edge` | Google | Que el contenido no quede tapado por las barras del sistema |
| `testing-setup` | Google | Infra de tests (unit, UI, screenshot, e2e) |
| `mobile-android-design` | wshobson | Criterio de diseño de UI móvil |

Globales en `~/.claude/skills/`: `android-cli` (Google — SDK, emulador, build) y
`find-skills` (buscar skills nuevas).

## Reglas

- Idioma de todo el material y de la interfaz: **español rioplatense**.
- **Nunca inventar datos clínicos.** Si un dato no está en el material de entrada ni en
  una guía oficial, se omite.
- Los HTML de `Materias/` son el producto final del generador — no editarlos acá a mano.
  Si hay que corregir contenido, se corrige en `..\Wordmed claude` y se regenera.
- No tomar decisiones de producto ni de arquitectura que Andrés no haya tomado.
  Si falta un dato, preguntar.

## Proyecto hermano

`C:\Users\andre\OneDrive\Documents\Wordmed claude` — el generador de cuadernillos.
Su `AGENTS.md` tiene las reglas clínicas obligatorias, la paleta, la estructura de
secciones y el pipeline técnico (`plantilla/construir.mjs`, `plantilla/exportar_docx.py`).

## Notas de entorno

- Node: v24.18.1 (`C:\Program Files\nodejs\node.exe`), npx disponible.
- Python (del proyecto hermano): `C:\Users\andre\AppData\Local\Programs\Python\Python312\python.exe`.
  Anteponerlo al `PATH` — el stub de Microsoft Store lo tapa si se agrega al final.
