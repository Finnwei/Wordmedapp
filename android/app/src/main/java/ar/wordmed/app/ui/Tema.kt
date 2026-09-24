package ar.wordmed.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ar.wordmed.app.R

/**
 * La misma paleta y la misma tipografía que los cuadernillos. La cáscara
 * y el contenido tienen que sentirse una sola cosa.
 */

data class Tinta(
    val papel: Color,
    val hoja: Color,
    val hojaAlt: Color,
    val hojaHund: Color,
    val linea: Color,
    val lineaFuerte: Color,
    val tinta: Color,
    val tintaMedia: Color,
    val tintaTenue: Color,
    /** Los colores de las secciones del cuadernillo, en orden: general,
     *  fisiopatología, clínica, diagnóstico, tratamiento, importante. Es el
     *  degradado de la barra de avance, y el que llevan las notas. */
    val espectro: List<Color>,
)

val TINTA_CLARA = Tinta(
    papel = Color(0xFFF5F7F9), hoja = Color(0xFFFFFFFF), hojaAlt = Color(0xFFFAFBFC),
    hojaHund = Color(0xFFEFF3F6), linea = Color(0xFFDFE5EB), lineaFuerte = Color(0xFFC2CBD5),
    tinta = Color(0xFF323232), tintaMedia = Color(0xFF5A6472), tintaTenue = Color(0xFF7C8797),
    espectro = listOf(
        Color(0xFF27AE60), Color(0xFF8E44AD), Color(0xFF2980B9),
        Color(0xFFD4AC0D), Color(0xFF008CBA), Color(0xFFC0392B),
    ),
)

val TINTA_OSCURA = Tinta(
    papel = Color(0xFF14171B), hoja = Color(0xFF1B1F25), hojaAlt = Color(0xFF20252C),
    hojaHund = Color(0xFF111418), linea = Color(0xFF2D343D), lineaFuerte = Color(0xFF414A56),
    tinta = Color(0xFFDDE3EA), tintaMedia = Color(0xFFA3ADBA), tintaTenue = Color(0xFF7F8B99),
    espectro = listOf(
        Color(0xFF4ED88A), Color(0xFFC387E8), Color(0xFF62B4EE),
        Color(0xFFE8C63C), Color(0xFF3FC2EC), Color(0xFFF2705F),
    ),
)

val LocalTinta = staticCompositionLocalOf { TINTA_CLARA }

private val opcionesOpenSans = { peso: Int ->
    Font(
        R.font.open_sans,
        FontWeight(peso),
        variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
    )
}

val Titulo = FontFamily(
    Font(R.font.spectral_regular, FontWeight.Normal),
    Font(R.font.spectral_bold, FontWeight.Bold),
)

val Texto = FontFamily(
    opcionesOpenSans(400),
    opcionesOpenSans(600),
    opcionesOpenSans(700),
)

val Dato = FontFamily.Monospace

private val tipografia = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Titulo, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontFamily = Titulo, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = Titulo, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = Titulo, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = Titulo, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = Titulo, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge.copy(fontFamily = Texto),
        bodyMedium = bodyMedium.copy(fontFamily = Texto),
        bodySmall = bodySmall.copy(fontFamily = Texto),
        labelLarge = labelLarge.copy(fontFamily = Texto),
        labelMedium = labelMedium.copy(fontFamily = Texto),
        labelSmall = labelSmall.copy(fontFamily = Texto),
    )
}

/** Rótulo en versalitas espaciadas, como los del cuadernillo. */
val estiloRotulo = TextStyle(
    fontFamily = Texto,
    fontSize = 11.sp,
    fontWeight = FontWeight(700),
    letterSpacing = 1.9.sp,
)

val estiloPestana = TextStyle(
    fontFamily = Dato,
    fontSize = 10.5.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.3.sp,
)

@Composable
fun TemaWordmed(oscuro: Boolean?, contenido: @Composable () -> Unit) {
    val esOscuro = oscuro ?: isSystemInDarkTheme()
    val t = if (esOscuro) TINTA_OSCURA else TINTA_CLARA

    val esquema = if (esOscuro) {
        darkColorScheme(
            background = t.papel, surface = t.hoja,
            onBackground = t.tinta, onSurface = t.tinta,
            primary = Color(0xFF62B4EE), onPrimary = Color(0xFF12161A),
            surfaceVariant = t.hojaHund, onSurfaceVariant = t.tintaMedia,
            outline = t.lineaFuerte, outlineVariant = t.linea,
        )
    } else {
        lightColorScheme(
            background = t.papel, surface = t.hoja,
            onBackground = t.tinta, onSurface = t.tinta,
            primary = Color(0xFF2980B9), onPrimary = Color.White,
            surfaceVariant = t.hojaHund, onSurfaceVariant = t.tintaMedia,
            outline = t.lineaFuerte, outlineVariant = t.linea,
        )
    }

    CompositionLocalProvider(LocalTinta provides t) {
        MaterialTheme(colorScheme = esquema, typography = tipografia, content = contenido)
    }
}
