package ar.wordmed.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Los mismos 112 colores y 8 estilos que la web. Si se cambia algo acá,
 * cambiarlo también en web/app.js: no comparten código, solo criterio.
 */

data class GrupoColor(val nombre: String, val tonos: List<String>)

val PALETAS = listOf(
    GrupoColor("Clínicos", listOf(
        "#27AE60","#1B8348","#16A085","#0E7C66","#2980B9","#1F6797","#008CBA","#00708F",
        "#8E44AD","#7D399A","#D4AC0D","#8E7300","#C0392B","#A62F22","#505050","#7C8797")),
    GrupoColor("Pasteles", listOf(
        "#A8D5BA","#B8E0D2","#C6E2E9","#A7C7E7","#B5C7ED","#C9B6E4","#E0C3E8","#F7C8D0",
        "#F9C6C6","#F9D5A7","#FDF0B2","#E9EFC0","#D6E5BD","#E8DCC8","#E4C1B9","#D9D9E3")),
    GrupoColor("Vivos", listOf(
        "#E74C3C","#FF5722","#E67E22","#FF9800","#F1C40F","#FFC107","#8BC34A","#2ECC71",
        "#1ABC9C","#00BCD4","#3498DB","#2196F3","#673AB7","#9B59B6","#E91E63","#FF4081")),
    GrupoColor("Tierra", listOf(
        "#A0522D","#C1440E","#9C6644","#A67B5B","#B7791F","#7D6608","#8B7355","#6E4B3A",
        "#5D4037","#6B8E23","#7F8C4F","#8A9A5B","#B08968","#C9A227","#A68A64","#7C6F57")),
    GrupoColor("Joya", listOf(
        "#0F766E","#115E59","#134E4A","#164E63","#1E3A8A","#1E40AF","#312E81","#4C1D95",
        "#581C87","#6B21A8","#3B0764","#831843","#9F1239","#7F1D1D","#92400E","#065F46")),
    GrupoColor("Neutros", listOf(
        "#2C3E50","#34495E","#343A40","#495057","#4A4A4A","#5A6472","#6D6875","#6B705C",
        "#737373","#7C8797","#8D99AE","#95A5A6","#A3A3A3","#ADB5BD","#B2BEC3","#C2CBD5")),
    GrupoColor("Neón", listOf(
        "#39FF14","#01FF89","#00FFC8","#B6FF00","#CCFF00","#FFFF00","#FFD300","#FF6B00",
        "#FF073A","#FF10F0","#FE01B1","#BC13FE","#7B2FFF","#04D9FF","#00F5FF","#7DF9FF")),
)

enum class EstiloCarpeta(val id: String, val nombre: String) {
    ORIGINAL("original", "Original"),
    VIDRIO("vidrio", "Vidrio"),
    ARCILLA("arcilla", "Arcilla"),
    FUTURISTA("futurista", "Futurista"),
    PAPEL("papel", "Papel"),
    SOLIDO("solido", "Sólido"),
    CONTORNO("contorno", "Contorno"),
    RELIEVE("relieve", "Relieve");

    companion object {
        fun de(id: String?) = entries.firstOrNull { it.id == id } ?: ORIGINAL
    }
}

/** El color con el que arranca cada materia si el usuario no eligió otro. */
val COLOR_POR_DEFECTO = mapOf(
    "infecto" to "#27AE60",
    "medicina-interna-3" to "#8E44AD",
    "psiquiatria" to "#008CBA",
    "obstetricia" to "#D4AC0D",
)
val RUEDA_POR_DEFECTO = listOf(
    "#2980B9", "#C0392B", "#16A085", "#D4AC0D", "#8E44AD", "#008CBA", "#E67E22", "#505050",
)

/** Colores de los bloques dentro de una materia (Endocrino, Hemato, …). */
val COLOR_BLOQUE = listOf("#8E44AD", "#C0392B", "#D4AC0D", "#2980B9", "#27AE60", "#008CBA")

fun colorDe(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

/** Blanco o tinta oscura, según cuánto ilumina el fondo. */
fun sobre(c: Color): Color = if (c.luminance() > 0.42f) Color(0xFF1A1A1A) else Color.White

fun mezclar(c: Color, fondo: Color, proporcion: Float): Color = Color(
    red = c.red * proporcion + fondo.red * (1 - proporcion),
    green = c.green * proporcion + fondo.green * (1 - proporcion),
    blue = c.blue * proporcion + fondo.blue * (1 - proporcion),
)
