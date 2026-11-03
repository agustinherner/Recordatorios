package com.example.recordatorios.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/** Situaciones que comunica el panel. */
enum class TipoPanel { Carga, Vacio, Error }

/**
 * Componente reutilizable para comunicar CARGA, VACÍO o ERROR.
 *
 * La acción es opcional y llega como lambda: en el listado vacío es
 * "Crear recordatorio" y en el error es "Reintentar". El panel no sabe qué
 * hace esa acción, solo la ofrece.
 */
@Composable
fun PanelEstado(
    tipo: TipoPanel,
    mensaje: String,
    modifier: Modifier = Modifier,
    textoAccion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            // liveRegion: TalkBack anuncia el mensaje cuando aparece (carga/error).
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (tipo) {
            TipoPanel.Carga -> CircularProgressIndicator()
            TipoPanel.Vacio -> Icon(
                Icons.Filled.Info, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)
            )
            TipoPanel.Error -> Icon(
                Icons.Filled.Warning, contentDescription = null,   // el texto ya explica el error
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = if (tipo == TipoPanel.Error) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (textoAccion != null && onAccion != null) {
            Button(onClick = onAccion) { Text(textoAccion) }
        }
    }
}

@Preview(showBackground = true, name = "Panel · Error")
@Composable
private fun PreviewPanelError() {
    RecordatoriosTheme {
        PanelEstado(TipoPanel.Error, "No pudimos cargar los recordatorios.", textoAccion = "Reintentar", onAccion = {})
    }
}
