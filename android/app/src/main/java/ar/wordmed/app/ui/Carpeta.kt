package ar.wordmed.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * La carpeta de una materia, en cualquiera de los ocho estilos.
 * El color lo elige el usuario; el estilo dice cómo se dibuja.
 */
@Composable
fun Carpeta(
    nombre: String,
    etiqueta: String,
    descripcion: String?,
    color: Color,
    estilo: EstiloCarpeta,
    vacia: Boolean = false,
    espacioRueda: Boolean = false,
    alturaMinima: androidx.compose.ui.unit.Dp = 132.dp,
    compacta: Boolean = false,
    modifier: Modifier = Modifier,
    alTocar: (() -> Unit)? = null,
    adorno: @Composable (BoxScopeLike.() -> Unit)? = null,
) {
    val t = LocalTinta.current
    val c = if (vacia) t.lineaFuerte else color
    val enc = sobre(c)

    val radio = when (estilo) {
        EstiloCarpeta.VIDRIO -> 13.dp
        EstiloCarpeta.ARCILLA -> 22.dp
        EstiloCarpeta.RELIEVE -> 14.dp
        EstiloCarpeta.PAPEL -> 2.dp
        else -> 3.dp
    }
    val forma = RoundedCornerShape(radio)

    var caja = modifier
        .then(if (estilo == EstiloCarpeta.PAPEL) Modifier.rotate(-1.1f) else Modifier)
        .then(
            when (estilo) {
                EstiloCarpeta.ARCILLA -> Modifier.shadow(10.dp, forma, ambientColor = c, spotColor = c)
                EstiloCarpeta.RELIEVE -> Modifier.shadow(8.dp, forma)
                EstiloCarpeta.PAPEL -> Modifier.shadow(3.dp, forma)
                else -> Modifier
            }
        )
        .clip(forma)

    caja = when (estilo) {
        EstiloCarpeta.VIDRIO -> caja.background(
            Brush.linearGradient(
                listOf(
                    c.copy(alpha = 0.34f),
                    c.copy(alpha = 0.11f),
                    mezclar(c, Color.White, 0.26f).copy(alpha = 0.5f),
                )
            )
        ).border(1.dp, Color.White.copy(alpha = 0.55f), forma)

        EstiloCarpeta.ARCILLA -> caja.background(mezclar(c, t.hoja, 0.46f))

        EstiloCarpeta.FUTURISTA -> caja
            .background(Color(0xFF0B1016))
            .border(1.dp, c, forma)

        EstiloCarpeta.PAPEL -> caja
            .background(mezclar(Color(0xFFF2EADA), t.hoja, 0.55f))
            .border(1.dp, mezclar(t.tinta, t.hoja, 0.18f), forma)

        EstiloCarpeta.SOLIDO -> caja.background(c)

        EstiloCarpeta.CONTORNO -> caja.border(2.dp, c, forma)

        EstiloCarpeta.RELIEVE -> caja.background(t.hojaHund)

        EstiloCarpeta.ORIGINAL -> caja
            .background(if (vacia) t.hoja else mezclar(c, t.hoja, 0.08f))
            .border(1.dp, if (vacia) t.lineaFuerte else t.linea, forma)
    }

    if (alTocar != null) caja = caja.clickable(onClick = alTocar)

    val colorNombre = when (estilo) {
        EstiloCarpeta.SOLIDO -> enc
        EstiloCarpeta.CONTORNO -> mezclar(c, t.tinta, 0.72f)
        EstiloCarpeta.FUTURISTA -> mezclar(c, Color.White, 0.55f)
        EstiloCarpeta.VIDRIO -> mezclar(c, Color.Black, 0.72f)
        EstiloCarpeta.ARCILLA -> mezclar(c, Color.Black, 0.62f)
        else -> if (vacia) t.tintaTenue else t.tinta
    }

    Box(caja.defaultMinSize(minHeight = alturaMinima)) {
        PestanaCarpeta(etiqueta, c, enc, estilo, t)

        Column(
            Modifier
                .fillMaxWidth()
                /* En modo editar la rueda ocupa la esquina de arriba, así que el
                   texto empieza más abajo en vez de angostarse y partirse. */
                .padding(
                    start = if (compacta) 10.dp else 14.dp,
                    end = if (compacta) 10.dp else 14.dp,
                    top = when {
                        compacta -> 26.dp
                        espacioRueda -> 62.dp
                        else -> 34.dp
                    },
                    bottom = if (compacta) 10.dp else 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                nombre,
                fontFamily = if (estilo == EstiloCarpeta.FUTURISTA) Dato else Titulo,
                fontWeight = FontWeight.Bold,
                fontSize = when {
                    compacta -> 15.sp
                    estilo == EstiloCarpeta.FUTURISTA -> 15.sp
                    else -> 19.sp
                },
                lineHeight = if (compacta) 18.sp else 22.sp,
                color = colorNombre,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (descripcion != null) {
                Text(
                    descripcion,
                    fontFamily = Texto,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    color = when (estilo) {
                        EstiloCarpeta.SOLIDO -> enc.copy(alpha = 0.8f)
                        EstiloCarpeta.FUTURISTA -> mezclar(c, Color.White, 0.32f)
                        else -> t.tintaTenue
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        adorno?.invoke(BoxScopeLike(this))
    }
}

/** La solapa con la cantidad de temas. Cambia de forma según el estilo. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.PestanaCarpeta(
    etiqueta: String, c: Color, enc: Color, estilo: EstiloCarpeta, t: Tinta,
) {
    when (estilo) {
        EstiloCarpeta.VIDRIO, EstiloCarpeta.ARCILLA, EstiloCarpeta.RELIEVE ->
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 13.dp, top = 11.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        when (estilo) {
                            EstiloCarpeta.VIDRIO -> Color.White.copy(alpha = 0.58f)
                            EstiloCarpeta.ARCILLA -> mezclar(c, Color.Black, 0.88f)
                            else -> t.hojaHund
                        }
                    )
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    etiqueta.uppercase(), style = estiloPestana,
                    color = when (estilo) {
                        EstiloCarpeta.ARCILLA -> Color.White
                        else -> mezclar(c, t.tinta, 0.72f)
                    },
                )
            }

        EstiloCarpeta.SOLIDO ->
            Box(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp)) {
                Text(etiqueta.uppercase(), style = estiloPestana, color = enc.copy(alpha = 0.78f))
            }

        EstiloCarpeta.CONTORNO ->
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 11.dp)
                    .background(t.papel)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(etiqueta.uppercase(), style = estiloPestana, color = mezclar(c, t.tinta, 0.78f))
            }

        else ->
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(c)
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(etiqueta.uppercase(), style = estiloPestana, color = enc)
            }
    }
}

/** Envoltorio para poder pasar adornos posicionados desde afuera. */
class BoxScopeLike(val scope: androidx.compose.foundation.layout.BoxScope)
