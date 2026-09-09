package ar.wordmed.app.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import java.io.File

/**
 * El visor: el cuadernillo tal cual, dentro de un WebView.
 *
 * Se sirve por WebViewAssetLoader y no por file://, para que el origen
 * sea https y el localStorage del cuadernillo —donde guarda el modo
 * claro/oscuro— funcione de verdad.
 *
 * La barra de arriba es nativa y no se mueve al scrollear. El botón de
 * tema que trae el cuadernillo quedaba fuera de cuadro en pantalla chica
 * y se perdía al bajar, así que el envoltorio lo esconde cuando detecta
 * que lo está abriendo la app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Lector(
    ruta: String,
    titulo: String,
    materia: String,
    oscuro: Boolean,
    tamanoLetra: Int,
    alCambiarTamano: (Int) -> Unit,
    alRestablecerTamano: () -> Unit,
    alAlternarTema: () -> Unit,
    alProgreso: (String, Int, Int) -> Unit,
    alVolver: () -> Unit,
) {
    val t = LocalTinta.current
    var panelLetra by remember { mutableStateOf(false) }
    var barraVisible by remember { mutableStateOf(true) }

    BackHandler {
        when {
            panelLetra -> panelLetra = false
            !barraVisible -> barraVisible = true
            else -> alVolver()
        }
    }

    /* El WebView va primero y la barra después: un AndroidView se dibuja
       por encima de todo lo que se componga antes que él, aunque no se
       superpongan en el layout. Componiendo la barra al final queda
       arriba, y el WebView arranca más abajo por el padding. */
    var altoBarra by remember { mutableStateOf(0) }
    val densidad = LocalDensity.current

    Box(
        Modifier
            .fillMaxSize()
            .background(t.papel)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = with(densidad) { altoBarra.toDp() }),
            factory = { c ->
                val cargador = WebViewAssetLoader.Builder()
                    .addPathHandler(
                        "/contenido/",
                        WebViewAssetLoader.InternalStoragePathHandler(c, File(c.filesDir, "contenido"))
                    )
                    .build()

                WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false

                    /* Que se vea como al abrir el archivo a mano en el celular:
                       el envoltorio no emite <meta viewport> cuando detecta la
                       app, así que el WebView maqueta a su ancho por omisión y
                       lo encoge para que entre. Ese encogido es también el tope
                       de zoom out, que es lo que se pidió. */
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    settings.textZoom = tamanoLetra
                    isVerticalScrollBarEnabled = true

                    addJavascriptInterface(PuenteLector(alProgreso), "WordmedApp")

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView, request: WebResourceRequest,
                        ): WebResourceResponse? = cargador.shouldInterceptRequest(request.url)

                        /* Los enlaces internos (#seccion) se resuelven acá adentro;
                           cualquier cosa que apunte afuera no se abre. */
                        override fun shouldOverrideUrlLoading(
                            view: WebView, request: WebResourceRequest,
                        ): Boolean = request.url.host != "appassets.androidplatform.net"

                        override fun onPageFinished(view: WebView, url: String) {
                            view.evaluateJavascript(guionTema(oscuro), null)
                        }
                    }

                    loadUrl("https://appassets.androidplatform.net/contenido/$ruta")
                }
            },
            update = { v ->
                v.settings.textZoom = tamanoLetra
                v.evaluateJavascript(guionTema(oscuro), null)
            },
        )

        Column(
            Modifier
                .align(Alignment.TopStart)
                .background(t.papel)
                .onGloballyPositioned { altoBarra = it.size.height }
        ) {

        /* --- barra fija, arriba del nombre de la materia --- */
        AnimatedVisibility(
            visible = barraVisible,
            enter = expandVertically(tween(220)) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
        ) {
        Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonVisor(alVolver) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                    tint = t.tintaMedia, modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    materia.uppercase(), style = estiloRotulo, color = t.tintaTenue,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    titulo, fontFamily = Titulo, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, lineHeight = 18.sp, color = t.tinta,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            BotonVisor({ panelLetra = !panelLetra }) {
                Text(
                    "Aa", fontFamily = Titulo, fontWeight = FontWeight.Bold,
                    fontSize = 17.sp, color = if (panelLetra) t.tinta else t.tintaMedia,
                )
            }
            BotonVisor(alAlternarTema) {
                Icon(
                    if (oscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                    "Cambiar tema", tint = t.tintaMedia, modifier = Modifier.size(21.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = panelLetra,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            BarraTamanoLetra(
                valor = tamanoLetra,
                alCambiar = alCambiarTamano,
                alRestablecer = alRestablecerTamano,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 6.dp),
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(t.linea))
        }
        }

        }

        /* La flecha flota sobre el cuadernillo: no vive en la columna, así
           que no empuja el contenido ni ocupa una franja propia. Sin fondo,
           solo el ícono. */
        Box(
            Modifier
                /* A la derecha y no al centro: en este teléfono la cámara
                   está justo en el medio de arriba y el dedo la tapaba. */
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, altoBarra) }
                .padding(end = 4.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable { barraVisible = !barraVisible },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (barraVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                if (barraVisible) "Ocultar la barra" else "Mostrar la barra",
                tint = t.tintaTenue.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun guionTema(oscuro: Boolean) =
    "document.documentElement.setAttribute('data-theme','${if (oscuro) "dark" else "light"}');"

@Composable
private fun BotonVisor(alTocar: () -> Unit, contenido: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable(onClick = alTocar),
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

/**
 * Lo que el cuadernillo puede llamar desde JavaScript. El puente que va
 * dentro del envoltorio busca justamente `window.WordmedApp`.
 */
class PuenteLector(private val alProgreso: (String, Int, Int) -> Unit) {
    @JavascriptInterface
    fun progreso(clave: String, pct: Int, y: Int) = alProgreso(clave, pct, y)
}
