package com.example.recordatorios.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.ContenedorDependencias
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.etiqueta
import com.example.recordatorios.ui.componentes.Etiqueta
import com.example.recordatorios.ui.componentes.PanelEstado
import com.example.recordatorios.ui.componentes.TipoPanel
import com.example.recordatorios.ui.componentes.colorDeCategoria
import com.example.recordatorios.ui.componentes.colorDePrioridad
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/** Destino de detalle: recibe el id de la ruta y crea el ViewModel con ese id. */
@Composable
fun DetalleDestino(
    id: Long,
    onVolver: () -> Unit,
    viewModel: DetalleViewModel = viewModel { DetalleViewModel(id, ContenedorDependencias.repositorio) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetalleScreen(uiState = uiState, onVolver = onVolver, onReintentar = viewModel::onReintentar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al listado")
                    }
                }
            )
        }
    ) { innerPadding ->
        val contenidoModifier = Modifier.padding(innerPadding)
        when (uiState) {
            DetalleUiState.Cargando -> PanelEstado(TipoPanel.Carga, "Cargando recordatorio…", contenidoModifier)
            is DetalleUiState.Error -> PanelEstado(
                tipo = TipoPanel.Error,
                mensaje = uiState.mensaje,
                modifier = contenidoModifier,
                textoAccion = "Reintentar",
                onAccion = onReintentar
            )
            is DetalleUiState.Contenido -> DetalleContenido(uiState.recordatorio, contenidoModifier)
        }
    }
}

@Composable
private fun DetalleContenido(recordatorio: Recordatorio, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(recordatorio.titulo, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Etiqueta(recordatorio.categoria.nombre, colorDeCategoria(recordatorio))
            Etiqueta("Prioridad ${recordatorio.prioridad.etiqueta}", colorDePrioridad(recordatorio.prioridad))
        }
        HorizontalDivider()

        // Estado: si está completado se muestra también la fecha (dato propio de esa variante).
        val estado = recordatorio.estado
        val textoEstado = when (estado) {
            is EstadoRecordatorio.Completado -> "${estado.etiqueta()} el ${estado.fechaCompletado}"
            else -> estado.etiqueta()
        }
        FilaDato("Estado", textoEstado)
        FilaDato("Descripción", recordatorio.descripcion ?: "Sin descripción")
        // Datos opcionales: si son null se informa que no hay dato (null safety con ?:).
        FilaDato("Fecha límite", recordatorio.fechaLimite ?: "Sin fecha")
        FilaDato("Ubicación", recordatorio.ubicacion ?: "Sin ubicación")
        FilaDato("Aviso", recordatorio.aviso ?: "Sin aviso")
        FilaDato("Identificador", "#${recordatorio.id}")
    }
}

/** Par etiqueta / valor del detalle. mergeDescendants hace que TalkBack lea ambos juntos. */
@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true, name = "Detalle")
@Composable
private fun PreviewDetalle() {
    RecordatoriosTheme {
        DetalleScreen(DetalleUiState.Contenido(DatosSimulados.recordatorios.first()), {}, {})
    }
}
