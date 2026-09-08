package ar.wordmed.app.datos

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ar.wordmed.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val Context.ajustes by preferencesDataStore("wordmed")

/**
 * Guarda los cuadernillos en el disco del teléfono y los mantiene al día.
 *
 * La regla es la de la sección 3 del contexto: al abrir se baja el
 * manifiesto —unos KB— y si el hash_global no cambió, no se descarga
 * nada más. Si cambió, se bajan solo los archivos cuyo hash difiere.
 */
class Deposito(private val ctx: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val raiz = File(ctx.filesDir, "contenido")

    private val kHashGlobal = stringPreferencesKey("hash_global")
    private val kHashes = stringPreferencesKey("hashes")
    private val kProgreso = stringPreferencesKey("progreso")
    private val kCarpetas = stringPreferencesKey("carpetas")
    private val kTema = stringPreferencesKey("tema")
    private val kVistos = stringPreferencesKey("vistos")

    val carpetaContenido: File get() = raiz

    /* ---------- lectura local ---------- */

    fun archivoLocal(ruta: String) = File(raiz, ruta)

    fun hayContenido(): Boolean = File(raiz, "manifest.json").exists()

    suspend fun manifiestoLocal(): Manifiesto? = withContext(Dispatchers.IO) {
        val f = File(raiz, "manifest.json")
        if (!f.exists()) return@withContext null
        runCatching { json.decodeFromString<Manifiesto>(f.readText()) }.getOrNull()
    }

    suspend fun indiceLocal(): IndiceBusqueda? = withContext(Dispatchers.IO) {
        val f = File(raiz, "buscador.json")
        if (!f.exists()) return@withContext null
        runCatching { json.decodeFromString<IndiceBusqueda>(f.readText()) }.getOrNull()
    }

    /* ---------- sincronización ---------- */

    sealed interface Paso {
        data object Comprobando : Paso
        data class Bajando(val hechos: Int, val total: Int) : Paso
        data class Listo(val cambios: Int) : Paso
        data class SinCambios(val motivo: String) : Paso
        data class Falla(val motivo: String) : Paso
    }

    /**
     * @param alAvanzar se llama en cada paso para que la pantalla muestre el progreso.
     */
    suspend fun sincronizar(alAvanzar: (Paso) -> Unit) = withContext(Dispatchers.IO) {
        alAvanzar(Paso.Comprobando)

        val crudo = runCatching { bajarTexto("manifest.json") }.getOrElse {
            alAvanzar(
                if (hayContenido()) Paso.SinCambios("sin conexión")
                else Paso.Falla("No se pudo conectar y todavía no hay nada guardado.")
            )
            return@withContext
        }

        val remoto = runCatching { json.decodeFromString<Manifiesto>(crudo) }.getOrElse {
            alAvanzar(Paso.Falla("El manifiesto vino con un formato que no entiendo."))
            return@withContext
        }

        val hashLocal = leer(kHashGlobal)
        if (hashLocal == remoto.hash_global && hayContenido()) {
            alAvanzar(Paso.SinCambios("al día"))
            return@withContext
        }

        /* qué archivos cambiaron de verdad */
        val hashes = mapaDe(leer(kHashes))
        val pendientes = remoto.archivos().filter { a ->
            hashes[a.ruta] != a.hash || !archivoLocal(a.ruta).exists()
        }

        /* las fuentes viajan con el contenido: sin ellas el cuadernillo
           se ve con las del sistema, que es justo lo que evitamos */
        val fuentes = if (File(raiz, "fuentes/fuentes.css").exists()) emptyList()
        else runCatching { listarFuentes() }.getOrDefault(emptyList())

        val total = pendientes.size + fuentes.size + 2
        var hechos = 0
        alAvanzar(Paso.Bajando(hechos, total))

        try {
            for (f in fuentes) {
                bajarArchivo(f); hechos++; alAvanzar(Paso.Bajando(hechos, total))
            }
            for (a in pendientes) {
                bajarArchivo(a.ruta); hechos++; alAvanzar(Paso.Bajando(hechos, total))
                hashes[a.ruta] = a.hash
            }
            bajarArchivo("buscador.json"); hechos++; alAvanzar(Paso.Bajando(hechos, total))
            guardarTexto("manifest.json", crudo); hechos++

            /* los temas que ya no están en el manifiesto se borran */
            val validas = remoto.archivos().map { it.ruta }.toSet()
            File(raiz, "temas").walkBottomUp().forEach { f ->
                if (f.isFile) {
                    val rel = f.relativeTo(raiz).invariantSeparatorsPath
                    if (rel !in validas) { f.delete(); hashes.remove(rel) }
                }
            }

            escribir(kHashes, textoDe(hashes))
            escribir(kHashGlobal, remoto.hash_global)
            alAvanzar(Paso.Listo(pendientes.size))
        } catch (e: Exception) {
            alAvanzar(Paso.Falla(e.message ?: "Falló la descarga."))
        }
    }

    /** El CSS de las fuentes nombra sus .woff2: de ahí sale la lista. */
    private fun listarFuentes(): List<String> {
        val css = bajarTexto("fuentes/fuentes.css")
        guardarTexto("fuentes/fuentes.css", css)
        return Regex("""url\('\./([^']+\.woff2)'\)""")
            .findAll(css).map { "fuentes/" + it.groupValues[1] }.distinct().toList()
    }

    private fun bajarTexto(ruta: String): String =
        abrir(ruta).use { it.readBytes().toString(Charsets.UTF_8) }

    private fun bajarArchivo(ruta: String) {
        val destino = File(raiz, ruta)
        destino.parentFile?.mkdirs()
        val temporal = File(destino.parentFile, destino.name + ".parcial")
        abrir(ruta).use { entrada -> temporal.outputStream().use { entrada.copyTo(it) } }
        if (!temporal.renameTo(destino)) {
            temporal.copyTo(destino, overwrite = true); temporal.delete()
        }
    }

    private fun guardarTexto(ruta: String, texto: String) {
        val f = File(raiz, ruta)
        f.parentFile?.mkdirs()
        f.writeText(texto)
    }

    private fun abrir(ruta: String) = (URL("${BuildConfig.ORIGEN}/$ruta").openConnection() as HttpURLConnection).run {
        connectTimeout = 15000
        readTimeout = 30000
        setRequestProperty("Accept-Encoding", "gzip")
        if (responseCode !in 200..299) {
            disconnect(); throw java.io.IOException("$ruta respondió $responseCode")
        }
        if (getHeaderField("Content-Encoding")?.contains("gzip") == true)
            java.util.zip.GZIPInputStream(inputStream)
        else inputStream
    }

    /* ---------- preferencias ---------- */

    private suspend fun leer(k: androidx.datastore.preferences.core.Preferences.Key<String>): String =
        ctx.ajustes.data.first()[k] ?: ""

    private suspend fun escribir(
        k: androidx.datastore.preferences.core.Preferences.Key<String>, v: String,
    ) { ctx.ajustes.edit { it[k] = v } }

    /* Mapas guardados como "clave=valor" por línea: más simple de leer
       a mano que un JSON si alguna vez hay que depurarlo. */
    private fun mapaDe(texto: String): MutableMap<String, String> =
        texto.lineSequence().mapNotNull { l ->
            val i = l.indexOf('='); if (i <= 0) null else l.substring(0, i) to l.substring(i + 1)
        }.toMap(mutableMapOf())

    private fun textoDe(m: Map<String, String>) = m.entries.joinToString("\n") { "${it.key}=${it.value}" }

    /* ---------- progreso de lectura ---------- */

    data class Avance(val pct: Int, val y: Int, val ts: Long)

    suspend fun progreso(): Map<String, Avance> = mapaDe(leer(kProgreso)).mapValues { (_, v) ->
        val p = v.split(",")
        Avance(p.getOrNull(0)?.toIntOrNull() ?: 0, p.getOrNull(1)?.toIntOrNull() ?: 0, p.getOrNull(2)?.toLongOrNull() ?: 0L)
    }

    suspend fun anotarProgreso(clave: String, pct: Int, y: Int) {
        val m = mapaDe(leer(kProgreso))
        m[clave] = "$pct,$y,${System.currentTimeMillis()}"
        escribir(kProgreso, textoDe(m))
    }

    suspend fun reciente(): Pair<String, Avance>? =
        progreso().entries.maxByOrNull { it.value.ts }?.let { it.key to it.value }

    /* ---------- marca "Actualizado" ---------- */

    suspend fun vistos(): Map<String, String> = mapaDe(leer(kVistos))

    suspend fun marcarVisto(clave: String, hash: String) {
        val m = mapaDe(leer(kVistos)); m[clave] = hash; escribir(kVistos, textoDe(m))
    }

    /* ---------- personalización ---------- */

    data class Carpeta(val color: String?, val estilo: String?, val orden: Int?)

    suspend fun carpetas(): Map<String, Carpeta> = mapaDe(leer(kCarpetas)).mapValues { (_, v) ->
        val p = v.split("|")
        Carpeta(p.getOrNull(0)?.ifBlank { null }, p.getOrNull(1)?.ifBlank { null }, p.getOrNull(2)?.toIntOrNull())
    }

    suspend fun guardarCarpeta(slug: String, c: Carpeta) {
        val m = mapaDe(leer(kCarpetas))
        m[slug] = "${c.color ?: ""}|${c.estilo ?: ""}|${c.orden ?: ""}"
        escribir(kCarpetas, textoDe(m))
    }

    /* ---------- tema claro / oscuro ---------- */

    suspend fun temaOscuro(): Boolean? = when (leer(kTema)) {
        "dark" -> true
        "light" -> false
        else -> null
    }

    suspend fun guardarTema(oscuro: Boolean) = escribir(kTema, if (oscuro) "dark" else "light")
}
