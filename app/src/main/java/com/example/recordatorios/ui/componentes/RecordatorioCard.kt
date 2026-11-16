package com.example.recordatorios.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.estaPendiente
import com.example.recordatorios.domain.etiqueta
import com.example.recordatorios.domain.tieneCambiosSinConfirmar
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * Tarjeta reutilizable de un recordatorio: título, categoría, prioridad y estado.
 *
 * - Recibe los datos por parámetro: no consulta el repositorio.
 * - Comunica la selección con onClick: no conoce rutas ni el NavController.
 * - Acepta un modifier con valor por defecto para que quien la usa la ajuste.
 */
@Composable
fun RecordatorioCard(
    recordatorio: Recordatorio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorCategoria = colorDeCategoria(recordatorio)
    val completado = !recordatorio.estaPendiente()

    // Card con onClick: Material 3 ya le da el rol de botón para la accesibilidad.
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja lateral con el color de la categoría.
            Box(Modifier.width(6.dp).fillMaxHeight().background(colorCategoria))
            Column(Modifier.padding(12.dp).weight(1f)) {
                Text(
                    text = recordatorio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (completado) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Etiqueta(recordatorio.categoria.nombre, colorCategoria)
                    Etiqueta("Prioridad ${recordatorio.prioridad.etiqueta}", colorDePrioridad(recordatorio.prioridad))
                    Text(
                        text = recordatorio.estado.etiqueta(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Actividad 3: indica si el cambio todavía no fue confirmado por el servidor.
                if (recordatorio.tieneCambiosSinConfirmar()) {
                    Spacer(Modifier.height(8.dp))
                    EtiquetaSincronizacion(recordatorio.estadoSync)
                }
            }
        }
    }
}

/** Etiqueta de color reutilizada en la tarjeta y en el detalle. */
@Composable
fun Etiqueta(texto: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))   // primero recorte y fondo, después el relleno
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * Etiqueta del estado de sincronización. Se comunica con TEXTO y no solo con
 * color: "Pendiente de sincronizar" o "Error de sincronización".
 */
@Composable
fun EtiquetaSincronizacion(estadoSync: EstadoSync, modifier: Modifier = Modifier) {
    val color = if (estadoSync == EstadoSync.ERROR) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.tertiary
    Etiqueta(texto = estadoSync.etiqueta(), color = color, modifier = modifier)
}

/** El color sale del Map categoría → color de la Actividad 1. */
fun colorDeCategoria(recordatorio: Recordatorio): Color = Color(DatosSimulados.colorDe(recordatorio.categoria))

/** when exhaustivo sobre la sealed class Prioridad. */
@Composable
fun colorDePrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.Alta -> MaterialTheme.colorScheme.error
    Prioridad.Media -> Color(0xFFF9A825)
    Prioridad.Baja -> MaterialTheme.colorScheme.tertiary
}

@Preview(showBackground = true)
@Composable
private fun PreviewRecordatorioCard() {
    RecordatoriosTheme {
        RecordatorioCard(
            DatosSimulados.recordatorios.first().copy(estadoSync = EstadoSync.PENDIENTE),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
