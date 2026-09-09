package ar.wordmed.app.datos

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

class Estado(app: Application) : AndroidViewModel(app) {

    val deposito = Deposito(app)

    var manifiesto by mutableStateOf<Manifiesto?>(null); private set
    var paso by mutableStateOf<Deposito.Paso>(Deposito.Paso.Comprobando); private set
    var progreso by mutableStateOf<Map<String, Deposito.Avance>>(emptyMap()); private set
    var vistos by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var carpetas by mutableStateOf<Map<String, Deposito.Carpeta>>(emptyMap()); private set
    var oscuro by mutableStateOf<Boolean?>(null); private set
    var tamanoLetra by mutableStateOf(Deposito.LETRA_ORIGINAL); private set

    var editando by mutableStateOf(false)
    var materiaEnPanel by mutableStateOf<String?>(null)

    private var indice: IndiceBusqueda? = null
    var buscando by mutableStateOf(false); private set

    init {
        viewModelScope.launch {
            oscuro = deposito.temaOscuro()
            tamanoLetra = deposito.tamanoLetra()
            carpetas = deposito.carpetas()
            progreso = deposito.progreso()
            vistos = deposito.vistos()
            manifiesto = deposito.manifiestoLocal()
            sincronizar()
        }
    }

    fun sincronizar() = viewModelScope.launch {
        deposito.sincronizar { p -> paso = p }
        deposito.manifiestoLocal()?.let { manifiesto = it }
        indice = null
    }

    fun refrescarProgreso() = viewModelScope.launch {
        progreso = deposito.progreso()
        vistos = deposito.vistos()
    }

    fun anotarProgreso(clave: String, pct: Int, y: Int) = viewModelScope.launch {
        deposito.anotarProgreso(clave, pct, y)
    }

    fun abrirTema(materia: String, tema: Tema) = viewModelScope.launch {
        deposito.marcarVisto(tema.clave(materia), tema.hash)
        vistos = deposito.vistos()
    }

    fun alternarTema() = viewModelScope.launch {
        val nuevo = !(oscuro ?: false)
        deposito.guardarTema(nuevo)
        oscuro = nuevo
    }

    fun fijarTamanoLetra(v: Int) = viewModelScope.launch {
        tamanoLetra = v.coerceIn(Deposito.LETRA_MIN, Deposito.LETRA_MAX)
        deposito.guardarTamanoLetra(tamanoLetra)
    }

    fun restablecerTamanoLetra() = fijarTamanoLetra(Deposito.LETRA_ORIGINAL)

    fun fijarColor(slug: String, color: String) = guardarCarpeta(slug) { it.copy(color = color) }
    fun fijarEstilo(slug: String, estilo: String) = guardarCarpeta(slug) { it.copy(estilo = estilo) }

    fun reordenar(orden: List<String>) = viewModelScope.launch {
        orden.forEachIndexed { i, slug ->
            val c = carpetas[slug] ?: Deposito.Carpeta(null, null, null)
            deposito.guardarCarpeta(slug, c.copy(orden = i))
        }
        carpetas = deposito.carpetas()
    }

    private fun guardarCarpeta(slug: String, cambio: (Deposito.Carpeta) -> Deposito.Carpeta) =
        viewModelScope.launch {
            val actual = carpetas[slug] ?: Deposito.Carpeta(null, null, null)
            deposito.guardarCarpeta(slug, cambio(actual))
            carpetas = deposito.carpetas()
        }

    /** Materias en el orden que eligió el usuario. */
    fun materiasOrdenadas(): List<Materia> {
        val m = manifiesto ?: return emptyList()
        return m.materias.withIndex()
            .sortedBy { (i, mat) -> carpetas[mat.slug]?.orden ?: i }
            .map { it.value }
    }

    fun temaActualizado(materia: String, tema: Tema): Boolean {
        val clave = tema.clave(materia)
        val visto = vistos[clave] ?: return false
        return visto != tema.hash && progreso.containsKey(clave)
    }

    /* ---------- búsqueda ---------- */

    suspend fun buscar(consulta: String, dentroDe: String?): List<Hallazgo> {
        val m = manifiesto ?: return emptyList()
        val q = normalizar(consulta.trim())
        if (q.length < 2) return emptyList()

        if (indice == null) {
            buscando = true
            indice = deposito.indiceLocal()
            buscando = false
        }
        val idx = indice ?: return emptyList()

        return withContext(Dispatchers.Default) {
            val salida = ArrayList<Hallazgo>()
            for (e in idx.entradas) {
                if (dentroDe != null && e.materia != dentroDe) continue
                val enTitulo = normalizar(e.titulo).contains(q)
                val pos = normalizar(e.texto).indexOf(q)
                if (!enTitulo && pos == -1) continue

                val mat = m.materia(e.materia) ?: continue
                val tem = m.tema(e.materia, e.tema) ?: continue
                val (extracto, desde) = recortar(e.texto, q, pos)

                salida += Hallazgo(
                    entrada = e,
                    nombreMateria = mat.nombre,
                    tituloTema = tem.titulo,
                    destino = if (e.seccion != null) "${tem.archivo}#${e.seccion}" else tem.archivo,
                    extracto = extracto,
                    desde = desde,
                    largo = q.length,
                )
                if (salida.size >= 300) break
            }
            salida.sortedBy { if (normalizar(it.entrada.titulo).contains(q)) 0 else 1 }
        }
    }

    private fun recortar(texto: String, q: String, pos: Int): Pair<String, Int> {
        if (texto.isEmpty()) return "" to -1
        if (pos < 0) return texto.take(150) to -1
        val desde = maxOf(0, pos - 70)
        val hasta = minOf(texto.length, pos + q.length + 90)
        val trozo = (if (desde > 0) "…" else "") + texto.substring(desde, hasta) +
            (if (hasta < texto.length) "…" else "")
        return trozo to (pos - desde + if (desde > 0) 1 else 0)
    }

    companion object {
        fun normalizar(t: String): String =
            Normalizer.normalize(t, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase()
    }
}
