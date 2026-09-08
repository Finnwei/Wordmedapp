# Wordmed

Cuadernillos de estudio de medicina, organizados por materia, en dos destinos que comparten
el mismo contenido: una **web** y una **app Android**.

El contenido lo produce el proyecto hermano `Wordmed claude`, que transforma las
presentaciones de los profesores en cuadernillos HTML. Este repositorio los publica.

## Cómo está armado

```
Materias/          los cuadernillos como salen del generador (fragmentos sin <head>)
publicar/          los dos scripts que arman todo
contenido/         GENERADO — lo que se publica y sirve el host
  index.html         la cáscara web
  web/               su CSS y su JS
  temas/             los cuadernillos envueltos, listos para abrir
  fuentes/           Spectral, Open Sans y Cascadia Mono (SIL Open Font License)
  recursos/          PDF y demás material descargable por materia
  manifest.json      materias, temas, orden, hashes
  buscador.json      índice de búsqueda de texto completo
web/                el código fuente de la cáscara web
diseno/             las maquetas de la interfaz (.dc.html)
```

**`contenido/` es salida generada.** Se puede borrar entera y rehacerse con un comando.
Los HTML de `Materias/` no se editan acá: si hay que corregir un cuadernillo, se corrige en
el generador y se vuelve a copiar.

## Publicar

```bash
node publicar/publicar.mjs
```

Recorre `Materias/`, envuelve cada cuadernillo, arma el manifiesto y el índice de búsqueda,
y copia la cáscara web. Después, `git push`: el host redespliega solo.

Las fuentes se bajan una sola vez (y de nuevo solo si cambian):

```bash
node publicar/fuentes.mjs
```

## Por qué los cuadernillos se envuelven

Los archivos de `Materias/` son **fragmentos**, no documentos completos: no tienen
`<!DOCTYPE>`, `<html>` ni `<head>`. Están escritos con el formato que exige un Artifact, y
agregarles una cabecera los rompería como Artifact.

Sin `<meta viewport>` un celular los muestra a 980 px de ancho, microscópicos. Y las fuentes
que piden —Sitka, Segoe UI— son de Windows, no existen en Android y no se pueden
redistribuir. Por eso el script los envuelve al publicar: les agrega la cabecera, apunta a
las fuentes libres equivalentes y suma el puente que recuerda dónde quedó la lectura.

El original nunca se toca.

## El contrato entre la web y la app

Las dos leen los mismos dos archivos:

- **`manifest.json`** — materias, temas, orden, bloques y un `hash` por tema, más un
  `hash_global` que resume todo.
- **`buscador.json`** — una entrada por sección, con su texto sin etiquetas.

La app guarda el `hash_global` que vio. Al abrirse baja el manifiesto —unos KB— y si el hash
no cambió, no descarga nada más. Si cambió, baja solo los temas cuyo hash difiere.

Cada cáscara tiene su propia interfaz; el contenido es lo único compartido.

## Entorno

- Node 18 o superior (probado en 24).
- Las fuentes viajan con el contenido, no se piden a ningún CDN: la app tiene que
  funcionar sin conexión.

Para leer el detalle de las decisiones y lo que queda pendiente, ver `CONTEXTO.md`.
