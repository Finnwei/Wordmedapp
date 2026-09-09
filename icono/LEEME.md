# Dejá acá el ícono de la app

Poné el archivo en esta carpeta con el nombre **`icono.png`** (o `icono.jpg`).
De acá lo tomo, lo adapto y lo dejo puesto como ícono de Wordmed.

## Lo que Android hace con la imagen

Android no muestra la imagen tal cual: le aplica **su propia máscara**. Según el
teléfono y el lanzador, la recorta en círculo, en cuadrado redondeado o en
"squircle". En un Samsung con One UI es un squircle.

Eso tiene dos consecuencias:

1. **El marco blanco redondeado que dibujaste se va a perder.** Android va a
   recortar por su cuenta y ese borde queda cortado o deformado. Si querés un
   filo claro alrededor, tiene que estar *adentro* de la zona segura, no en el
   borde de la imagen.
2. **El borde de la imagen se recorta.** De los 108 puntos de ancho que usa
   Android, solo los **66 del centro** están garantizados. Todo lo que quede
   afuera de ese centro puede desaparecer.

## Cómo conviene que esté la imagen

| | |
|---|---|
| Tamaño | 1024 × 1024 px, cuadrada |
| Formato | PNG mejor que JPG (el JPG ensucia los bordes del dibujo) |
| Fondo | Que llegue **hasta los cuatro bordes**, sin marco ni margen claro |
| El dibujo | Centrado y dentro de los **625 px centrales** |
| Zona de recorte | Los **200 px de cada borde** pueden desaparecer: que no haya nada importante ahí |

## Si no la podés reexportar

Dejala igual, como esté. Yo la recorto, le saco el marco blanco, estiro el fondo
hasta los bordes y escalo el dibujo para que entre en la zona segura. Solo tené
en cuenta que **el marco blanco no va a sobrevivir** en ningún caso: esa forma la
decide Android.
