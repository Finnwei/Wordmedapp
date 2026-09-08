package ar.wordmed.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.wordmed.app.datos.Estado
import ar.wordmed.app.datos.Hallazgo
import kotlinx.coroutines.delay

/**
 * El buscador. Es contextual: abierto adentro de una materia busca solo
 * ahí; abierto desde afuera busca en todas y aclara de cuál es cada
 * resultado.
 */
@Composable
fun Buscador(
    estado: Estado,
    dentroDe: String?,
    alAbrir: (Hallazgo) -> Unit,
    alCerrar: () -> Unit,
) {
    val t = LocalTinta.current
    var consulta by remember { mutableStateOf("") }
    var hallazgos by remember { mutableStateOf<List<Hallazgo>>(emptyList()) }
    var buscado by remember { mutableStateOf(false) }
    val foco = remember { FocusRequester() }
    val nombreMateria = dentroDe?.let { estado.manifiesto?.materia(it)?.nombre }

    LaunchedEffect(Unit) { foco.requestFocus() }

    LaunchedEffect(consulta) {
        if (consulta.trim().length < 2) { hallazgos = emptyList(); buscado = false; return@LaunchedEffect }
        delay(120)
        hallazgos = estado.buscar(consulta, dentroDe)
        buscado = true
    }

    Column(Modifier.fillMaxSize().background(t.papel).statusBarsPadding()) {

        Row(
            Modifier
                .fillMaxWidth()
                .background(t.hoja)
                .padding(14.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.weight(1f)) {
                if (consulta.isEmpty()) {
                    Text(
                        nombreMateria?.let { "Buscar en $it" } ?: "Buscar en todas las materias",
                        fontFamily = Texto, fontSize = 17.sp, color = t.tintaTenue,
                    )
                }
                BasicTextField(
                    value = consulta,
                    onValueChange = { consulta = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(foco),
                    textStyle = TextStyle(fontFamily = Texto, fontSize = 17.sp, color = t.tinta),
                    cursorBrush = SolidColor(t.tintaMedia),
                    singleLine = true,
                )
            }
            Text(
                "Cancelar",
                Modifier.clickable(onClick = alCerrar).padding(4.dp),
                fontFamily = Texto, fontSize = 14.sp, color = t.tintaMedia,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.lineaFuerte))

        when {
            consulta.trim().length < 2 -> Mensaje("Escribí al menos dos letras.")
            estado.buscando -> Mensaje("Preparando el índice…")
            hallazgos.isEmpty() && buscado -> Mensaje("Nada con “${consulta.trim()}”.")
            else -> LazyColumn(
                Modifier.imePadding(),
                contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val temas = hallazgos.map { it.entrada.tema }.distinct().size
                    Text(
                        "${hallazgos.size} RESULTADOS EN $temas TEMAS",
                        style = estiloRotulo, color = t.tintaTenue,
                    )
                }
                items(hallazgos.take(60)) { h -> TarjetaHallazgo(estado, h) { alAbrir(h) } }
            }
        }
    }
}

@Composable
private fun TarjetaHallazgo(estado: Estado, h: Hallazgo, alTocar: () -> Unit) {
    val t = LocalTinta.current
    val color = colorDe(
        estado.carpetas[h.entrada.materia]?.color
            ?: COLOR_POR_DEFECTO[h.entrada.materia] ?: "#2980B9"
    )
    val forma = RoundedCornerShape(3.dp)
    val marca = if (h.entrada.seccion != null) "${h.entrada.rotulo} · ${h.tituloTema}" else h.tituloTema

    Box(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(t.hoja)
            .border(1.dp, t.linea, forma)
            .clickable(onClick = alTocar)
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(color)
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                marca.uppercase(), style = estiloPestana, color = sobre(color),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(Modifier.padding(14.dp, 26.dp, 14.dp, 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                h.entrada.titulo.ifBlank { h.tituloTema },
                fontFamily = Titulo, fontWeight = FontWeight.Bold,
                fontSize = 17.sp, lineHeight = 20.sp, color = t.tinta,
            )
            Text(
                resaltar(h),
                fontFamily = Texto, fontSize = 13.sp, lineHeight = 19.sp, color = t.tintaMedia,
            )
            Text(h.nombreMateria, fontFamily = Texto, fontSize = 11.5.sp, color = t.tintaTenue)
        }
    }
}

/** Marca en negrita el pedazo que coincide con lo buscado. */
@Composable
private fun resaltar(h: Hallazgo): AnnotatedString {
    val t = LocalTinta.current
    if (h.desde < 0 || h.desde + h.largo > h.extracto.length) return AnnotatedString(h.extracto)
    return buildAnnotatedString {
        append(h.extracto.substring(0, h.desde))
        withStyle(SpanStyle(color = t.tinta, fontWeight = FontWeight.Bold)) {
            append(h.extracto.substring(h.desde, h.desde + h.largo))
        }
        append(h.extracto.substring(h.desde + h.largo))
    }
}

@Composable
private fun Mensaje(texto: String) {
    val t = LocalTinta.current
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.TopCenter) {
        Text(texto, fontFamily = Texto, fontSize = 14.sp, color = t.tintaTenue)
    }
}
