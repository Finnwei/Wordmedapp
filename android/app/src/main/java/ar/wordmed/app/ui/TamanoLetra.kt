package ar.wordmed.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.wordmed.app.datos.Deposito
import kotlin.math.roundToInt

/**
 * El control de tamaño de letra. Va en dos lugares: en la barra del
 * visor, detrás del botón Aa, y en el menú del principio.
 *
 * Mueve el textZoom del WebView: agranda el texto sin desarmar la
 * maquetación del cuadernillo. 100 es el tamaño original.
 */
@Composable
fun BarraTamanoLetra(
    valor: Int,
    alCambiar: (Int) -> Unit,
    alRestablecer: () -> Unit,
    modifier: Modifier = Modifier,
    conMarco: Boolean = true,
) {
    val t = LocalTinta.current
    val forma = RoundedCornerShape(3.dp)

    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (conMarco) Modifier
                    .clip(forma)
                    .background(t.hojaHund)
                    .border(1.dp, t.linea, forma)
                    .padding(12.dp, 10.dp)
                else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("A", fontFamily = Titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = t.tintaTenue)

            Slider(
                value = valor.toFloat(),
                onValueChange = { alCambiar(it.roundToInt()) },
                valueRange = Deposito.LETRA_MIN.toFloat()..Deposito.LETRA_MAX.toFloat(),
                steps = (Deposito.LETRA_MAX - Deposito.LETRA_MIN) / 5 - 1,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = t.tinta,
                    activeTrackColor = t.tintaMedia,
                    inactiveTrackColor = t.linea,
                    activeTickColor = t.hoja,
                    inactiveTickColor = t.linea,
                ),
            )

            Text("A", fontFamily = Titulo, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = t.tintaMedia)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$valor %",
                Modifier.weight(1f),
                fontFamily = Dato, fontSize = 11.sp, color = t.tintaTenue,
            )
            if (valor != Deposito.LETRA_ORIGINAL) {
                Text(
                    "Volver al original",
                    Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .clickable(onClick = alRestablecer)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    fontFamily = Texto, fontWeight = FontWeight(600),
                    fontSize = 12.sp, color = t.tintaMedia,
                )
            }
        }
    }
}
