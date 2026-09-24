package ar.wordmed.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.wordmed.app.datos.Nota
import ar.wordmed.app.datos.Notas
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/** El lado del cuadradito, y el grosor de su filo de color. */
private val LADO = 26.dp
private val FILO = 2.5.dp

/**
 * Las notas del cuadernillo abierto: la lista, cuál está abierta, y las
 * operaciones que las cambian. Todo lo que toca el disco pasa por acá.
 */
@Stable
class EstadoNotas(
    private val almacen: Notas,
    private val clave: String,
    private val enSegundoPlano: (suspend () -> Unit) -> Unit,
) {
    var notas by mutableStateOf<List<Nota>>(emptyList()); private set
    var abierta by mutableStateOf<String?>(null)

    /** Una nota recién creada abre el teclado; las guardadas, no. */
    var reciencreada by mutableStateOf<String?>(null)

    fun cargar() = enSegundoPlano { notas = almacen.de(clave) }

    private fun persistir(nuevas: List<Nota>) {
        notas = nuevas
        enSegundoPlano { almacen.guardar(clave, nuevas) }
    }

    fun agregar(x: Float, y: Float) {
        val nota = Nota(id = UUID.randomUUID().toString(), x = x, y = y)
        persistir(notas + nota)
        reciencreada = nota.id
        abierta = nota.id
    }

    fun mover(id: String, x: Float, y: Float) =
        persistir(notas.map { if (it.id == id) it.copy(x = x, y = y) else it })

    fun escribir(id: String, texto: String) =
        persistir(notas.map { if (it.id == id) it.copy(texto = texto) else it })

    fun agregarImagen(id: String) = enSegundoPlano {
        val nombre = almacen.pegarImagen() ?: return@enSegundoPlano
        persistir(notas.map { if (it.id == id) it.copy(imagenes = it.imagenes + nombre) else it })
    }

    fun quitarImagen(id: String, nombre: String) {
        persistir(notas.map { if (it.id == id) it.copy(imagenes = it.imagenes - nombre) else it })
        enSegundoPlano { almacen.barrerImagenesHuerfanas() }
    }

    fun borrar(id: String) {
        abierta = null
        persistir(notas.filterNot { it.id == id })
        enSegundoPlano { almacen.barrerImagenesHuerfanas() }
    }

    fun archivoImagen(nombre: String): File = almacen.imagen(nombre)
}

@Composable
fun recordarNotas(clave: String): EstadoNotas {
    val ctx = LocalContext.current
    val alcance = rememberCoroutineScope()
    val almacen = remember(ctx) { Notas(ctx) }
    val estado = remember(clave) {
        EstadoNotas(almacen, clave) { tarea -> alcance.launch { tarea() } }
    }
    LaunchedEffect(clave) { estado.cargar() }
    return estado
}

/**
 * La capa de cuadraditos, encima del cuadernillo.
 *
 * Las notas guardan su lugar en coordenadas del documento; acá se traducen
 * a coordenadas de pantalla con el zoom y el scroll del WebView. Por eso
 * la capa tiene que ocupar exactamente el mismo rectángulo que él.
 */
@Composable
fun CapaNotas(
    estado: EstadoNotas,
    escala: Float,
    desplazamientoX: Int,
    desplazamientoY: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clipToBounds()) {
        for (nota in estado.notas) {
            key(nota.id) {
                Marca(
                    nota = nota,
                    escala = escala,
                    desplazamientoX = desplazamientoX,
                    desplazamientoY = desplazamientoY,
                    alAbrir = { estado.abierta = nota.id },
                    alSoltar = { x, y -> estado.mover(nota.id, x, y) },
                )
            }
        }
    }
}

@Composable
private fun Marca(
    nota: Nota,
    escala: Float,
    desplazamientoX: Int,
    desplazamientoY: Int,
    alAbrir: () -> Unit,
    alSoltar: (Float, Float) -> Unit,
) {
    val t = LocalTinta.current
    var arrastre by remember { mutableStateOf(Offset.Zero) }
    var arrastrando by remember { mutableStateOf(false) }

    val x = (nota.x * escala).roundToInt() - desplazamientoX
    val y = (nota.y * escala).roundToInt() - desplazamientoY

    Box(
        Modifier
            .offset {
                IntOffset(x + arrastre.x.roundToInt(), y + arrastre.y.roundToInt())
            }
            .size(LADO)
            /* Hueco: sólo el filo lleva color, el centro deja ver el texto
               que hay debajo. */
            .border(BorderStroke(FILO, Brush.linearGradient(t.espectro)), RoundedCornerShape(5.dp))
            .pointerInput(nota.id) {
                detectTapGestures { alAbrir() }
            }
            .pointerInput(nota.id) {
                /* La posición se escribe recién al soltar. Moverla en pleno
                   arrastre recrea el nodo y mata el gesto: es el mismo
                   problema que tuvieron las carpetas. */
                detectDragGesturesAfterLongPress(
                    onDragStart = { arrastrando = true },
                    onDrag = { cambio, delta ->
                        cambio.consume()
                        arrastre += delta
                    },
                    onDragEnd = {
                        alSoltar(
                            nota.x + arrastre.x / escala,
                            nota.y + arrastre.y / escala,
                        )
                        arrastre = Offset.Zero
                        arrastrando = false
                    },
                    onDragCancel = {
                        arrastre = Offset.Zero
                        arrastrando = false
                    },
                )
            }
    ) {
        /* Mientras se arrastra, el centro se tiñe apenas para que se note
           que la nota está levantada. */
        if (arrastrando) {
            Box(Modifier.matchParentSize().background(t.hoja.copy(alpha = 0.55f)))
        }
    }
}

/**
 * La nota abierta.
 *
 * Deja de ser multicolor: pasa a ser una hoja, con el color sólo en el filo
 * izquierdo, igual que la pestaña de cada sección del cuadernillo.
 */
@Composable
fun PanelNota(
    estado: EstadoNotas,
    nota: Nota,
    modifier: Modifier = Modifier,
) {
    val t = LocalTinta.current
    var editando by remember(nota.id) { mutableStateOf(estado.reciencreada == nota.id) }
    var borrador by remember(nota.id) { mutableStateOf(nota.texto) }

    val foco = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current

    LaunchedEffect(editando) {
        if (editando) {
            foco.requestFocus()
            teclado?.show()
        } else {
            teclado?.hide()
        }
    }

    /* Abierta deja de ser multicolor y pasa a ser una hoja. El color queda
       sólo en el filo izquierdo: el degradado va de fondo del panel entero
       y la hoja lo tapa salvo esos 5dp, igual que la pestaña de cada
       sección del cuadernillo. */
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(t.espectro))
    ) {
        Column(
            Modifier
                .padding(start = 5.dp)
                .fillMaxWidth()
                .background(t.hoja)
                .padding(14.dp, 12.dp)
        ) {

            if (editando) {
                BasicTextField(
                    value = borrador,
                    onValueChange = { borrador = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 220.dp)
                        .focusRequester(foco),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = Texto, fontSize = 15.sp, lineHeight = 22.sp, color = t.tinta,
                    ),
                    cursorBrush = SolidColor(t.tinta),
                    decorationBox = { campo ->
                        if (borrador.isEmpty()) {
                            Text(
                                "Escribí tu apunte",
                                fontFamily = Texto, fontSize = 15.sp, color = t.tintaTenue,
                            )
                        }
                        campo()
                    },
                )
            } else if (nota.texto.isBlank() && nota.imagenes.isEmpty()) {
                Text(
                    "Nota vacía. Tocá el lápiz para escribirla.",
                    fontFamily = Texto, fontSize = 14.sp, color = t.tintaTenue,
                )
            } else if (nota.texto.isNotBlank()) {
                Text(
                    nota.texto,
                    Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                    fontFamily = Texto, fontSize = 15.sp, lineHeight = 22.sp, color = t.tinta,
                )
            }

            if (nota.imagenes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (nombre in nota.imagenes) {
                        key(nombre) {
                            ImagenDeNota(
                                archivo = estado.archivoImagen(nombre),
                                alQuitar = if (editando) {
                                    { estado.quitarImagen(nota.id, nombre) }
                                } else null,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(t.linea))
            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editando) {
                    BotonNota("Pegar imagen", { estado.agregarImagen(nota.id) }) {
                        Icon(
                            Icons.Default.ContentPaste, "Pegar imagen",
                            tint = t.tintaMedia, modifier = Modifier.size(19.dp),
                        )
                    }
                } else {
                    BotonNota("Editar", { editando = true }) {
                        Icon(
                            Icons.Default.Edit, "Editar",
                            tint = t.tintaMedia, modifier = Modifier.size(19.dp),
                        )
                    }
                }

                BotonNota("Borrar", { estado.borrar(nota.id) }) {
                    Icon(
                        Icons.Default.Delete, "Borrar la nota",
                        tint = t.tintaTenue, modifier = Modifier.size(19.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    if (editando) "OK" else "Cerrar",
                    Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .clickable {
                            if (editando) {
                                estado.escribir(nota.id, borrador)
                                estado.reciencreada = null
                                editando = false
                            } else {
                                estado.abierta = null
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    fontFamily = Titulo, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = t.tinta,
                )
            }
        }
    }
}

@Composable
private fun BotonNota(rotulo: String, alTocar: () -> Unit, contenido: @Composable () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable(onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

/**
 * Una imagen pegada. Se decodifica achicada: las fotos del teléfono son de
 * varios miles de píxeles y en la nota se ven a un par de cientos.
 */
@Composable
private fun ImagenDeNota(archivo: File, alQuitar: (() -> Unit)?) {
    val t = LocalTinta.current
    val mapa by produceState<ImageBitmap?>(null, archivo.path) {
        value = runCatching {
            val medir = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(archivo.path, medir)
            val opciones = BitmapFactory.Options().apply {
                inSampleSize = generateSequence(1) { it * 2 }
                    .first { medir.outWidth / it <= 1200 }
            }
            BitmapFactory.decodeFile(archivo.path, opciones)?.asImageBitmap()
        }.getOrNull()
    }

    Box(Modifier.fillMaxWidth()) {
        mapa?.let {
            Image(
                bitmap = it,
                contentDescription = "Imagen de la nota",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(RoundedCornerShape(3.dp)),
                contentScale = ContentScale.Fit,
            )
        } ?: Box(
            Modifier.fillMaxWidth().height(60.dp)
                .clip(RoundedCornerShape(3.dp)).background(t.hojaHund)
        )

        alQuitar?.let { quitar ->
            Text(
                "Quitar",
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(t.hoja.copy(alpha = 0.9f))
                    .clickable(onClick = quitar)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontFamily = Texto, fontWeight = FontWeight(600),
                fontSize = 12.sp, color = t.tintaMedia,
            )
        }
    }
}
