package ar.wordmed.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import ar.wordmed.app.datos.Estado
import ar.wordmed.app.ui.App
import ar.wordmed.app.ui.TemaWordmed
import java.io.File

class MainActivity : ComponentActivity() {

    private val estado: Estado by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaWordmed(oscuro = estado.oscuro) {
                App(estado = estado, alAbrirRecurso = ::abrirRecurso)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        estado.refrescarProgreso()
    }

    /** Abre un PDF con el visor que el usuario ya tenga en el teléfono. */
    private fun abrirRecurso(ruta: String) {
        val f = File(File(filesDir, "contenido"), ruta)
        if (!f.exists()) return
        val uri = FileProvider.getUriForFile(this, "$packageName.archivos", f)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (ruta.endsWith(".pdf")) "application/pdf" else "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(Intent.createChooser(i, "Abrir con")) }
    }
}
