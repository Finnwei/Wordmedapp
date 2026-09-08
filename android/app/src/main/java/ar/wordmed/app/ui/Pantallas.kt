package ar.wordmed.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.wordmed.app.datos.Deposito
import ar.wordmed.app.datos.Estado
import ar.wordmed.app.datos.Materia
import ar.wordmed.app.datos.Tema

/* ============================================================
   Pantalla de materias
   ============================================================ */

@Composable
fun PantallaMaterias(
    estado: Estado,
    alEntrar: (String) -> Unit,
    alLeer: (String, Tema) -> Unit,
    alTocarRueda: (String) -> Unit,
) {
    val t = LocalTinta.current
    val materias = estado.materiasOrdenadas()
    val m = estado.manifiesto

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Text(
                "Wordmed",
                fontFamily = Titulo, fontWeight = FontWeight.Bold,
                fontSize = 34.sp, lineHeight = 36.sp, color = t.tinta,
            )
        }

        if (estado.editando) {
            item { AvisoEditar() }
        } else {
            recienteDe(estado)?.let { (materia, tema, avance) ->
                /* sin rótulo arriba: la pestaña de la ficha ya dice "Reciente" */
                item { FichaReciente(estado, materia, tema, avance) { alLeer(materia.slug, tema) } }
            }
        }

        item { Text("MATERIAS", style = estiloRotulo, color = t.tintaTenue) }

        items(materias.chunked(2)) { fila ->
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                for (mat in fila) {
                    val cfg = estado.carpetas[mat.slug]
                    val color = colorDe(
                        cfg?.color
                            ?: COLOR_POR_DEFECTO[mat.slug]
                            ?: RUEDA_POR_DEFECTO[materias.indexOf(mat) % RUEDA_POR_DEFECTO.size]
                    )
                    val vacia = mat.temas.isEmpty()
                    val bloques = mat.bloques.filterNotNull()

                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        Carpeta(
                            nombre = mat.nombre,
                            etiqueta = if (vacia) "Vacía" else "${mat.temas.size} temas",
                            descripcion = when {
                                vacia -> "sin material todavía"
                                bloques.isNotEmpty() -> bloques.joinToString(" · ")
                                else -> null
                            },
                            color = color,
                            estilo = if (vacia) EstiloCarpeta.ORIGINAL else EstiloCarpeta.de(cfg?.estilo),
                            vacia = vacia,
                            espacioRueda = estado.editando,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            alTocar = if (estado.editando) null else ({ alEntrar(mat.slug) }),
                        )
                        if (estado.editando) {
                            RuedaColor(
                                Modifier.align(Alignment.TopEnd).padding(top = 29.dp, end = 8.dp)
                            ) { alTocarRueda(mat.slug) }
                        }
                    }
                }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (m == null && estado.paso !is Deposito.Paso.Bajando) {
            item { EstadoVacio(textoDelPaso(estado.paso)) }
        }
    }
}

@Composable
private fun AvisoEditar() {
    val t = LocalTinta.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(t.hojaHund)
            .border(1.dp, t.lineaFuerte, RoundedCornerShape(3.dp))
            .padding(14.dp, 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Editar carpetas", fontFamily = Texto, fontWeight = FontWeight(700), fontSize = 13.5.sp, color = t.tinta)
            Text("Tocá la rueda para elegir color y estilo", fontFamily = Texto, fontSize = 11.5.sp, color = t.tintaTenue)
        }
    }
}

@Composable
private fun RuedaColor(modifier: Modifier, alTocar: () -> Unit) {
    val t = LocalTinta.current
    Box(
        modifier
            .size(30.dp)
            .clip(RoundedCornerShape(50))
            .background(
                androidx.compose.ui.graphics.Brush.sweepGradient(
                    listOf(
                        Color(0xFFE74C3C), Color(0xFFE67E22), Color(0xFFF1C40F), Color(0xFF2ECC71),
                        Color(0xFF1ABC9C), Color(0xFF3498DB), Color(0xFF9B59B6), Color(0xFFE91E63),
                        Color(0xFFE74C3C),
                    )
                )
            )
            .border(2.dp, t.hoja, RoundedCornerShape(50))
            .clickable(onClick = alTocar)
    )
}

@Composable
private fun FichaReciente(
    estado: Estado, materia: Materia, tema: Tema, avance: Deposito.Avance, alTocar: () -> Unit,
) {
    val t = LocalTinta.current
    val color = colorDe(
        estado.carpetas[materia.slug]?.color ?: COLOR_POR_DEFECTO[materia.slug] ?: "#8E44AD"
    )
    val forma = RoundedCornerShape(3.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(mezclar(color, t.hoja, 0.08f))
            .border(1.dp, t.linea, forma)
            .clickable(onClick = alTocar)
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(color)
                .padding(horizontal = 11.dp, vertical = 5.dp)
        ) { Text("RECIENTE", style = estiloPestana, color = sobre(color)) }

        Column(Modifier.padding(15.dp, 34.dp, 15.dp, 16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        tema.titulo, fontFamily = Titulo, fontWeight = FontWeight.Bold,
                        fontSize = 23.sp, lineHeight = 26.sp, color = t.tinta,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        materia.nombre + (tema.bloque?.let { " · $it" } ?: ""),
                        fontFamily = Texto, fontSize = 12.sp, color = t.tintaTenue,
                    )
                }
                Text(
                    "${avance.pct}%", fontFamily = Dato, fontWeight = FontWeight.Bold,
                    fontSize = 22.sp, color = mezclar(color, t.tinta, 0.76f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(t.hoja)) {
                Box(
                    Modifier
                        .fillMaxWidth(avance.pct / 100f)
                        .height(3.dp)
                        .background(color)
                )
            }
        }
    }
}

private fun recienteDe(estado: Estado): Triple<Materia, Tema, Deposito.Avance>? {
    val m = estado.manifiesto ?: return null
    val (clave, avance) = estado.progreso.entries.maxByOrNull { it.value.ts }
        ?.let { it.key to it.value } ?: return null
    val partes = clave.split("/")
    if (partes.size != 2) return null
    val mat = m.materia(partes[0]) ?: return null
    val tem = m.tema(partes[0], partes[1]) ?: return null
    return Triple(mat, tem, avance)
}

/* ============================================================
   Pantalla de una materia
   ============================================================ */

@Composable
fun PantallaTemas(
    estado: Estado,
    materia: Materia,
    alLeer: (Tema) -> Unit,
    alAbrirRecurso: (String) -> Unit,
) {
    val t = LocalTinta.current
    val color = colorDe(
        estado.carpetas[materia.slug]?.color ?: COLOR_POR_DEFECTO[materia.slug] ?: "#2980B9"
    )
    val bloques = materia.bloques

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { CabeceraMateria(materia, color, alAbrirRecurso) }

        if (materia.temas.isEmpty()) {
            item { EstadoVacio("Todavía no hay cuadernillos en esta materia.") }
        }

        bloques.forEachIndexed { i, bloque ->
            val cb = if (bloque != null) colorDe(COLOR_BLOQUE[i % COLOR_BLOQUE.size]) else color
            val temas = materia.temas.filter { it.bloque == bloque }
            if (temas.isEmpty()) return@forEachIndexed

            if (bloque != null) {
                item {
                    Row(
                        Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Box(Modifier.width(24.dp).height(2.dp).background(cb))
                        Text(bloque.uppercase(), style = estiloRotulo, color = mezclar(cb, t.tinta, 0.72f))
                        Box(Modifier.weight(1f).height(1.dp).background(t.linea))
                    }
                }
            }

            items(temas) { tema ->
                FilaTema(
                    tema = tema,
                    color = cb,
                    pct = estado.progreso[tema.clave(materia.slug)]?.pct,
                    actualizado = estado.temaActualizado(materia.slug, tema),
                    alTocar = { alLeer(tema) },
                )
            }
        }
    }
}

@Composable
private fun CabeceraMateria(materia: Materia, color: Color, alAbrirRecurso: (String) -> Unit) {
    val t = LocalTinta.current
    val forma = RoundedCornerShape(3.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(mezclar(color, t.hoja, 0.08f))
            .border(1.dp, t.linea, forma)
            .padding(15.dp, 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(color))
        Text(
            materia.nombre, fontFamily = Titulo, fontWeight = FontWeight.Bold,
            fontSize = 26.sp, lineHeight = 29.sp, color = t.tinta,
        )
        Text(
            buildString {
                append("${materia.temas.size} temas")
                val b = materia.bloques.filterNotNull().size
                if (b > 0) append(" · $b bloques")
            },
            fontFamily = Texto, fontSize = 11.5.sp, color = t.tintaTenue,
        )
        for (r in materia.recursos) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(t.hoja)
                    .border(1.dp, t.lineaFuerte, RoundedCornerShape(3.dp))
                    .clickable { alAbrirRecurso(r.archivo) }
                    .padding(13.dp, 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    r.nombre, fontFamily = Texto, fontWeight = FontWeight(600),
                    fontSize = 12.5.sp, color = t.tinta,
                )
                Text(
                    r.tipo.uppercase() + " · " + "%.0f KB".format(r.bytes / 1024.0),
                    fontFamily = Dato, fontSize = 10.sp, color = t.tintaTenue,
                )
            }
        }
    }
}

@Composable
private fun FilaTema(
    tema: Tema, color: Color, pct: Int?, actualizado: Boolean, alTocar: () -> Unit,
) {
    val t = LocalTinta.current
    val forma = RoundedCornerShape(3.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(t.hoja)
            .border(1.dp, t.linea, forma)
            .clickable(onClick = alTocar)
            .padding(13.dp, 8.dp)
            .defaultMinSize(minHeight = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier
                .defaultMinSize(minWidth = 26.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                tema.orden.ifBlank { "·" }, fontFamily = Dato, fontWeight = FontWeight.Bold,
                fontSize = 10.sp, color = sobre(color),
            )
        }
        Text(
            tema.titulo, Modifier.weight(1f),
            fontFamily = Titulo, fontWeight = FontWeight.Bold,
            fontSize = 16.sp, lineHeight = 19.sp, color = t.tinta,
        )
        when {
            actualizado -> Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(mezclar(color, t.hoja, 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "ACTUALIZADO", fontFamily = Texto, fontWeight = FontWeight(700),
                    fontSize = 9.sp, letterSpacing = 0.9.sp, color = mezclar(color, t.tinta, 0.72f),
                )
            }
            pct != null && pct > 0 -> Text(
                "$pct%", fontFamily = Dato, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, color = t.tintaTenue,
            )
        }
    }
}

@Composable
fun EstadoVacio(texto: String) {
    val t = LocalTinta.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, fontFamily = Texto, fontSize = 14.sp, color = t.tintaTenue)
    }
}

fun textoDelPaso(p: Deposito.Paso): String = when (p) {
    is Deposito.Paso.Comprobando -> "Buscando los cuadernillos…"
    is Deposito.Paso.Bajando -> "Bajando ${p.hechos} de ${p.total}…"
    is Deposito.Paso.Listo -> "Listo."
    is Deposito.Paso.SinCambios -> p.motivo
    is Deposito.Paso.Falla -> p.motivo
}
