package com.example.recordatorios.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * Avisos sobre el estado de los datos, ARRIBA de la lista (no en su lugar).
 *
 * En una app local-first la falta de conexión no es un error de pantalla: los
 * datos guardados se siguen mostrando y solo se informa que los cambios se
 * enviarán más tarde (Módulo 3 · U2 · Tema 2, "Actualización del estado de la interfaz").
 *
 * Muestra, según corresponda:
 * - sin conexión (con la cantidad de cambios en espera);
 * - cambios pendientes de sincronizar (con conexión);
 * - el resultado fallido de la última sincronización, con opción de reintentar.
 */
@Composable
fun BannerEstadoDatos(
    enLinea: Boolean,
    cambiosSinSincronizar: Int,
    mensajeSincronizacion: String?,
    esError: Boolean,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (!enLinea) {
            FilaAviso(
                icono = Icons.Filled.Warning,
                texto = "Estás trabajando sin conexión. " +
                    if (cambiosSinSincronizar > 0) "$cambiosSinSincronizar cambio(s) se sincronizarán más tarde."
                    else "Los cambios se sincronizarán más tarde.",
                fondo = MaterialTheme.colorScheme.secondaryContainer,
                contenido = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else if (mensajeSincronizacion != null) {
            FilaAviso(
                icono = Icons.Filled.Warning,
                texto = mensajeSincronizacion,
                fondo = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                contenido = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                textoAccion = "Reintentar",
                onAccion = onReintentar
            )
        } else if (cambiosSinSincronizar > 0) {
            FilaAviso(
                icono = Icons.Filled.Info,
                texto = "$cambiosSinSincronizar cambio(s) pendientes de sincronizar.",
                fondo = MaterialTheme.colorScheme.tertiaryContainer,
                contenido = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun FilaAviso(
    icono: ImageVector,
    texto: String,
    fondo: Color,
    contenido: Color,
    textoAccion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    Surface(color = fondo, contentColor = contenido, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icono, contentDescription = null)   // el texto ya explica el aviso
            Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (textoAccion != null && onAccion != null) {
                TextButton(onClick = onAccion) { Text(textoAccion) }
            }
        }
    }
}

@Preview(showBackground = true, name = "Banner · Sin conexión")
@Composable
private fun PreviewBannerOffline() {
    RecordatoriosTheme {
        BannerEstadoDatos(enLinea = false, cambiosSinSincronizar = 2, mensajeSincronizacion = null, esError = false, onReintentar = {})
    }
}
