package ar.wordmed.app.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import ar.wordmed.app.datos.Deposito
import java.io.File

/**
 * El visor: el cuadernillo tal cual, dentro de un WebView.
 *
 * Se sirve por WebViewAssetLoader y no por file://, para que el origen
 * sea https y el localStorage del cuadernillo —donde guarda el modo
 * claro/oscuro— funcione de verdad.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Lector(
    ruta: String,
    clave: String,
    oscuro: Boolean,
    deposito: Deposito,
    alProgreso: (String, Int, Int) -> Unit,
    alVolver: () -> Unit,
) {
    val ctx = LocalContext.current
    BackHandler(onBack = alVolver)

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                val cargador = WebViewAssetLoader.Builder()
                    .addPathHandler(
                        "/contenido/",
                        WebViewAssetLoader.InternalStoragePathHandler(
                            c, File(c.filesDir, "contenido")
                        )
                    )
                    .build()

                WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.textZoom = 100
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
                            view.evaluateJavascript(
                                "document.documentElement.setAttribute('data-theme','${if (oscuro) "dark" else "light"}');",
                                null,
                            )
                        }
                    }

                    loadUrl("https://appassets.androidplatform.net/contenido/$ruta")
                }
            },
            update = { web ->
                web.evaluateJavascript(
                    "document.documentElement.setAttribute('data-theme','${if (oscuro) "dark" else "light"}');",
                    null,
                )
            },
        )
    }
}

/**
 * Lo que el cuadernillo puede llamar desde JavaScript. El puente que va
 * dentro del envoltorio busca justamente `window.WordmedApp`: si existe,
 * esconde su propio botón de volver y reporta el avance por acá.
 */
class PuenteLector(private val alProgreso: (String, Int, Int) -> Unit) {
    @JavascriptInterface
    fun progreso(clave: String, pct: Int, y: Int) = alProgreso(clave, pct, y)
}
