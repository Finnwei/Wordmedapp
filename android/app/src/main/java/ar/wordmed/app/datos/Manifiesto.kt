package ar.wordmed.app.datos

import kotlinx.serialization.Serializable

/**
 * El contrato con la capa de contenido. Estas clases son el espejo exacto
 * de manifest.json y buscador.json, los mismos archivos que lee la web.
 */

@Serializable
data class Manifiesto(
    val generado: String = "",
    val hash_global: String = "",
    val materias: List<Materia> = emptyList(),
) {
    val totalTemas: Int get() = materias.sumOf { it.temas.size }

    fun materia(slug: String): Materia? = materias.firstOrNull { it.slug == slug }

    fun tema(materia: String, tema: String): Tema? =
        materia(materia)?.temas?.firstOrNull { it.slug == tema }

    /** Todos los archivos que la app tiene que tener guardados. */
    fun archivos(): List<ArchivoRemoto> = buildList {
        for (m in materias) {
            for (t in m.temas) add(ArchivoRemoto(t.archivo, t.hash))
            for (r in m.recursos) add(ArchivoRemoto(r.archivo, r.hash))
        }
    }
}

@Serializable
data class Materia(
    val slug: String,
    val nombre: String,
    val carpeta: String = "",
    val temas: List<Tema> = emptyList(),
    val recursos: List<Recurso> = emptyList(),
) {
    /** Los bloques en el orden en que aparecen los temas. */
    val bloques: List<String?> get() = temas.map { it.bloque }.distinct()
}

@Serializable
data class Tema(
    val orden: String = "",
    val slug: String,
    val titulo: String,
    val bloque: String? = null,
    val bajada: String = "",
    val archivo: String,
    val secciones: Int = 0,
    val bytes: Long = 0,
    val hash: String = "",
) {
    fun clave(materia: String) = "$materia/$slug"
}

@Serializable
data class Recurso(
    val nombre: String,
    val archivo: String,
    val tipo: String = "",
    val bytes: Long = 0,
    val hash: String = "",
)

data class ArchivoRemoto(val ruta: String, val hash: String)

/* ---------- índice de búsqueda ---------- */

@Serializable
data class IndiceBusqueda(
    val hash_global: String = "",
    val entradas: List<EntradaBusqueda> = emptyList(),
)

@Serializable
data class EntradaBusqueda(
    val materia: String,
    val tema: String,
    val seccion: String? = null,
    val rotulo: String = "",
    val titulo: String = "",
    val texto: String = "",
)

/** Un resultado ya resuelto contra el manifiesto, listo para pintar. */
data class Hallazgo(
    val entrada: EntradaBusqueda,
    val nombreMateria: String,
    val tituloTema: String,
    val destino: String,
    val extracto: String,
    val desde: Int,
    val largo: Int,
)
