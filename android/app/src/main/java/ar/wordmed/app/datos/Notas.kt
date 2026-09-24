package ar.wordmed.app.datos

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Una nota pegada sobre un cuadernillo.
 *
 * `fx` y `fy` son la posición como **fracción de la hoja**: 0 es el borde de
 * arriba o el de la izquierda, 1 el de abajo o el de la derecha. Guardada
 * así, la nota no depende del zoom para nada.
 *
 * El formato viejo —`x` e `y` en píxeles CSS— sí dependía: había que
 * convertirlos con el zoom del momento, que el WebView informa tarde y mal,
 * y una nota podía terminar guardada fuera de la hoja y no verse nunca más.
 * Se convierten solos la primera vez que se abre el cuadernillo.
 */
@Serializable
data class Nota(
    val id: String,
    val fx: Float? = null,
    val fy: Float? = null,
    val x: Float? = null,
    val y: Float? = null,
    val texto: String = "",
    val imagenes: List<String> = emptyList(),
)

/**
 * Dónde viven las notas.
 *
 * Fuera de `contenido/`, que la sincronización borra entero cada vez que
 * cambia el material: las notas son lo único del teléfono que no se puede
 * volver a bajar de ningún lado.
 *
 * Un archivo por cuadernillo, y las imágenes aparte en una carpeta común,
 * nombradas por su contenido para no duplicar la misma foto dos veces.
 */
class Notas(private val ctx: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val raiz = File(ctx.filesDir, "notas")
    private val carpetaImagenes = File(raiz, "imagenes")

    private fun archivo(clave: String) = File(raiz, "${clave.replace("/", "~")}.json")

    fun imagen(nombre: String) = File(carpetaImagenes, nombre)

    suspend fun de(clave: String): List<Nota> = withContext(Dispatchers.IO) {
        val f = archivo(clave)
        if (!f.exists()) return@withContext emptyList()
        runCatching { json.decodeFromString<List<Nota>>(f.readText()) }.getOrDefault(emptyList())
    }

    suspend fun guardar(clave: String, notas: List<Nota>) = withContext(Dispatchers.IO) {
        raiz.mkdirs()
        val f = archivo(clave)
        if (notas.isEmpty()) f.delete() else f.writeText(json.encodeToString(notas))
    }

    /**
     * Trae la imagen que haya en el portapapeles y la deja en el disco.
     * Devuelve el nombre del archivo, o null si lo que se copió no era una
     * imagen —texto, por ejemplo— o si no se pudo leer.
     */
    suspend fun pegarImagen(): String? = withContext(Dispatchers.IO) {
        val portapapeles = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = portapapeles?.primaryClip ?: return@withContext null

        for (i in 0 until clip.itemCount) {
            val uri = clip.getItemAt(i).uri ?: continue
            val tipo = ctx.contentResolver.getType(uri) ?: continue
            if (!tipo.startsWith("image/")) continue

            val datos = runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull() ?: continue

            carpetaImagenes.mkdirs()
            /* El nombre sale del contenido: si pega dos veces la misma
               imagen, se guarda una sola vez. */
            val nombre = "${datos.size}-${datos.contentHashCode().toUInt()}.img"
            val destino = File(carpetaImagenes, nombre)
            if (!destino.exists()) destino.writeBytes(datos)
            return@withContext nombre
        }
        null
    }

    /** Borra las imágenes que ya no menciona ninguna nota de ningún cuadernillo. */
    suspend fun barrerImagenesHuerfanas() = withContext(Dispatchers.IO) {
        if (!carpetaImagenes.isDirectory) return@withContext
        val enUso = buildSet {
            raiz.listFiles { f -> f.extension == "json" }?.forEach { f ->
                runCatching { json.decodeFromString<List<Nota>>(f.readText()) }
                    .getOrDefault(emptyList())
                    .forEach { addAll(it.imagenes) }
            }
        }
        carpetaImagenes.listFiles()?.forEach { if (it.name !in enUso) it.delete() }
    }
}
