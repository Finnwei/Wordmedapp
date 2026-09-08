package ar.wordmed.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.wordmed.app.datos.Deposito
import ar.wordmed.app.datos.Estado

sealed interface Ruta {
    data object Materias : Ruta
    data class Temas(val slug: String) : Ruta
    data class Leyendo(val ruta: String, val clave: String) : Ruta
}

@Composable
fun App(estado: Estado, alAbrirRecurso: (String) -> Unit) {
    val t = LocalTinta.current
    var ruta by remember { mutableStateOf<Ruta>(Ruta.Materias) }
    var cajonAbierto by remember { mutableStateOf(false) }
    var buscadorAbierto by remember { mutableStateOf(false) }

    val esOscuro = estado.oscuro ?: androidx.compose.foundation.isSystemInDarkTheme()

    /* --- el lector ocupa la pantalla entera --- */
    (ruta as? Ruta.Leyendo)?.let { r ->
        Lector(
            ruta = r.ruta,
            clave = r.clave,
            oscuro = esOscuro,
            deposito = estado.deposito,
            alProgreso = estado::anotarProgreso,
            alVolver = { estado.refrescarProgreso(); ruta = volverDe(r, ruta, estado) },
        )
        return
    }

    Box(Modifier.fillMaxSize().background(t.papel)) {

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            BarraSuperior(
                titulo = when (val r = ruta) {
                    is Ruta.Temas -> estado.manifiesto?.materia(r.slug)?.nombre ?: "Wordmed"
                    else -> "Wordmed"
                },
                atras = ruta is Ruta.Temas,
                alPrincipal = {
                    if (ruta is Ruta.Temas) ruta = Ruta.Materias else cajonAbierto = true
                },
                alBuscar = { buscadorAbierto = true },
            )

            when (val r = ruta) {
                is Ruta.Materias -> PantallaMaterias(
                    estado = estado,
                    alEntrar = { ruta = Ruta.Temas(it) },
                    alLeer = { slug, tema ->
                        estado.abrirTema(slug, tema)
                        ruta = Ruta.Leyendo(tema.archivo, tema.clave(slug))
                    },
                    alTocarRueda = { estado.materiaEnPanel = it },
                )

                is Ruta.Temas -> estado.manifiesto?.materia(r.slug)?.let { mat ->
                    PantallaTemas(
                        estado = estado,
                        materia = mat,
                        alLeer = { tema ->
                            estado.abrirTema(mat.slug, tema)
                            ruta = Ruta.Leyendo(tema.archivo, tema.clave(mat.slug))
                        },
                        alAbrirRecurso = alAbrirRecurso,
                    )
                }

                else -> Unit
            }
        }

        /* --- barra de estado de la sincronización --- */
        AnimatedVisibility(
            visible = estado.paso is Deposito.Paso.Bajando || estado.paso is Deposito.Paso.Falla,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(t.hojaHund)
                    .navigationBarsPadding()
                    .padding(16.dp, 12.dp)
            ) {
                Text(
                    textoDelPaso(estado.paso),
                    fontFamily = Texto, fontSize = 13.sp, color = t.tintaMedia,
                )
            }
        }

        /* --- botón OK del modo editar --- */
        if (estado.editando) {
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(18.dp, 22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF27AE60))
                    .clickable { estado.editando = false; estado.materiaEnPanel = null }
                    .padding(22.dp, 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("OK", fontFamily = Texto, fontWeight = FontWeight(700), fontSize = 15.sp, color = Color.White)
            }
        }

        /* --- cajón --- */
        if (cajonAbierto) {
            Velo { cajonAbierto = false }
            Cajon(estado) { cajonAbierto = false }
        }

        /* --- panel de color y estilo --- */
        estado.materiaEnPanel?.let { slug ->
            Velo { estado.materiaEnPanel = null }
            Panel(estado, slug) { estado.materiaEnPanel = null }
        }

        /* --- buscador --- */
        if (buscadorAbierto) {
            BackHandler { buscadorAbierto = false }
            Buscador(
                estado = estado,
                dentroDe = (ruta as? Ruta.Temas)?.slug,
                alAbrir = { h ->
                    estado.manifiesto?.tema(h.entrada.materia, h.entrada.tema)?.let { tema ->
                        estado.abrirTema(h.entrada.materia, tema)
                    }
                    buscadorAbierto = false
                    ruta = Ruta.Leyendo(h.destino, "${h.entrada.materia}/${h.entrada.tema}")
                },
                alCerrar = { buscadorAbierto = false },
            )
        }
    }

    if (ruta is Ruta.Temas && !cajonAbierto && !buscadorAbierto && estado.materiaEnPanel == null) {
        BackHandler { ruta = Ruta.Materias }
    }
}

/** Al salir del lector se vuelve a donde estábamos. */
private fun volverDe(r: Ruta.Leyendo, actual: Ruta, estado: Estado): Ruta {
    val materia = r.clave.substringBefore("/")
    return if (estado.manifiesto?.materia(materia) != null) Ruta.Temas(materia) else Ruta.Materias
}

@Composable
private fun BarraSuperior(titulo: String, atras: Boolean, alPrincipal: () -> Unit, alBuscar: () -> Unit) {
    val t = LocalTinta.current
    Row(
        Modifier.fillMaxWidth().padding(6.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonBarra(if (atras) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu, alPrincipal)
        Text(
            titulo.uppercase(),
            Modifier.weight(1f),
            style = estiloRotulo, color = t.tintaTenue,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        BotonBarra(Icons.Default.Search, alBuscar)
    }
}

@Composable
private fun BotonBarra(icono: androidx.compose.ui.graphics.vector.ImageVector, alTocar: () -> Unit) {
    val t = LocalTinta.current
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable(onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) { Icon(icono, null, tint = t.tintaMedia, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun Velo(alTocar: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x6B1C2836))
            .clickable(onClick = alTocar)
    )
}

@Composable
private fun Cajon(estado: Estado, alCerrar: () -> Unit) {
    val t = LocalTinta.current
    val m = estado.manifiesto
    BackHandler(onBack = alCerrar)

    Column(
        Modifier
            .fillMaxHeight()
            .width(296.dp)
            .background(t.hoja)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.padding(18.dp, 24.dp, 18.dp, 20.dp)) {
            Text("Wordmed", fontFamily = Titulo, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = t.tinta)
            Text(
                m?.let { "${it.totalTemas} temas en ${it.materias.size} materias" } ?: "sin contenido",
                fontFamily = Texto, fontSize = 11.5.sp, color = t.tintaTenue,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.linea))

        OpcionCajon(Icons.Default.DarkMode, "Modo oscuro", alTocar = { estado.alternarTema() }) {
            Switch(
                checked = estado.oscuro ?: androidx.compose.foundation.isSystemInDarkTheme(),
                onCheckedChange = { estado.alternarTema() },
            )
        }
        OpcionCajon(Icons.Outlined.FolderOpen, "Editar carpetas", alTocar = {
            estado.editando = true; alCerrar()
        })
        OpcionCajon(Icons.Default.Refresh, "Buscar actualizaciones", alTocar = {
            estado.sincronizar(); alCerrar()
        }) {
            Text(textoDelPaso(estado.paso), fontFamily = Texto, fontSize = 11.sp, color = t.tintaTenue)
        }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.linea))
        Column(Modifier.padding(18.dp, 16.dp)) {
            Text(
                "Contenido ${m?.hash_global ?: "—"}",
                fontFamily = Dato, fontSize = 10.5.sp, color = t.tintaTenue,
            )
        }
    }
}

@Composable
private fun OpcionCajon(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    alTocar: () -> Unit,
    accesorio: @Composable (() -> Unit)? = null,
) {
    val t = LocalTinta.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = alTocar)
            .padding(18.dp, 14.dp)
            .defaultMinSize(minHeight = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icono, null, tint = t.tintaMedia, modifier = Modifier.size(20.dp))
        Text(texto, Modifier.weight(1f), fontFamily = Texto, fontSize = 15.sp, color = t.tinta)
        accesorio?.invoke()
    }
}

/** Panel de color y estilo. Los 112 colores en cuadrícula, los 8 estilos aparte. */
@Composable
private fun BoxScope.Panel(estado: Estado, slug: String, alCerrar: () -> Unit) {
    val t = LocalTinta.current
    var solapa by remember { mutableStateOf(0) }
    val cfg = estado.carpetas[slug]
    val colorActual = cfg?.color ?: COLOR_POR_DEFECTO[slug] ?: "#2980B9"
    BackHandler(onBack = alCerrar)

    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(0.72f)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(t.hoja)
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 9.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(38.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(t.linea))
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp, 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                estado.manifiesto?.materia(slug)?.nombre ?: "",
                Modifier.weight(1f), fontFamily = Texto, fontSize = 13.sp, color = t.tintaTenue,
            )
            Text(colorActual.uppercase(), fontFamily = Dato, fontSize = 11.sp, color = t.tintaTenue)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("Colores", "Estilos").forEachIndexed { i, nombre ->
                Box(
                    Modifier
                        .weight(1f)
                        .clickable { solapa = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        nombre, fontFamily = Texto, fontWeight = FontWeight(600), fontSize = 13.5.sp,
                        color = if (solapa == i) t.tinta else t.tintaTenue,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.linea))

        if (solapa == 0) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 90.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                for (grupo in PALETAS) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                grupo.nombre.uppercase(),
                                fontFamily = Texto, fontWeight = FontWeight(700),
                                fontSize = 9.5.sp, letterSpacing = 1.7.sp, color = t.tintaTenue,
                            )
                            Box(Modifier.weight(1f).height(1.dp).background(t.linea))
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            grupo.tonos.chunked(8).forEach { fila ->
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    fila.forEach { hex ->
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(colorDe(hex))
                                                .then(
                                                    if (hex.equals(colorActual, true))
                                                        Modifier.border(2.dp, t.tintaMedia, RoundedCornerShape(3.dp))
                                                    else Modifier
                                                )
                                                .clickable { estado.fijarColor(slug, hex) }
                                        )
                                    }
                                    repeat(8 - fila.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(EstiloCarpeta.entries) { e ->
                    val elegido = e.id == (cfg?.estilo ?: "original")
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .border(
                                if (elegido) 2.dp else 1.dp,
                                if (elegido) t.tintaMedia else t.linea,
                                RoundedCornerShape(3.dp),
                            )
                            .clickable { estado.fijarEstilo(slug, e.id) }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(78.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(t.papel)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            /* miniatura: sin alto mínimo, si no el nombre se corta */
                            Carpeta(
                                nombre = "Aa", etiqueta = "8 temas", descripcion = null,
                                color = colorDe(colorActual), estilo = e,
                                alturaMinima = 0.dp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(e.nombre, fontFamily = Texto, fontWeight = FontWeight(600), fontSize = 12.sp, color = t.tinta)
                    }
                }
            }
        }
    }
}
