package com.example.recordatorios.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.recordatorios.domain.EstadoSync
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.etiqueta
import com.example.recordatorios.ui.componentes.Etiqueta
import com.example.recordatorios.ui.componentes.EtiquetaSincronizacion
import com.example.recordatorios.ui.componentes.PanelEstado
import com.example.recordatorios.ui.componentes.TipoPanel
import com.example.recordatorios.ui.componentes.colorDeCategoria
import com.example.recordatorios.ui.componentes.colorDePrioridad
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import java.text.DateFormat
import java.util.Date

/** Destino de detalle: recibe el id de la ruta y crea el ViewModel con ese id. */
@Composable
fun DetalleDestino(
    id: String,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    viewModel: DetalleViewModel = viewModel { DetalleViewModel(id, ContenedorDependencias.repositorio) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Si el recordatorio se eliminó (acá o desde otro dispositivo), se vuelve al listado.
    LaunchedEffect(uiState) {
        if (uiState is DetalleUiState.Eliminado) onVolver()
    }

    DetalleScreen(
        uiState = uiState,
        onVolver = onVolver,
        onReintentar = viewModel::onReintentar,
        onEditar = onEditar,
        onAvanzarEstado = viewModel::onAvanzarEstado,
        onEliminar = viewModel::onEliminar,
        onErrorMostrado = viewModel::onErrorMostrado
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    onEditar: () -> Unit,
    onAvanzarEstado: () -> Unit,
    onEliminar: () -> Unit,
    onErrorMostrado: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Confirmación de borrado: estado simple de UI que sobrevive a la rotación.
    var confirmarEliminacion by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val contenido = uiState as? DetalleUiState.Contenido
    LaunchedEffect(contenido?.mensajeError) {
        contenido?.mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            onErrorMostrado()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al listado")
                    }
                },
                actions = {
                    if (contenido != null) {
                        IconButton(onClick = onEditar, enabled = !contenido.procesando) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { confirmarEliminacion = true }, enabled = !contenido.procesando) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val contenidoModifier = Modifier.padding(innerPadding)
        when (uiState) {
            DetalleUiState.Cargando, DetalleUiState.Eliminado ->
                PanelEstado(TipoPanel.Carga, "Cargando recordatorio…", contenidoModifier)
            is DetalleUiState.Error -> PanelEstado(
                tipo = TipoPanel.Error,
                mensaje = uiState.mensaje,
                modifier = contenidoModifier,
                textoAccion = "Reintentar",
                onAccion = onReintentar
            )
            is DetalleUiState.Contenido -> DetalleContenido(
                recordatorio = uiState.recordatorio,
                procesando = uiState.procesando,
                onAvanzarEstado = onAvanzarEstado,
                modifier = contenidoModifier
            )
        }
    }

    if (confirmarEliminacion) {
        AlertDialog(
            onDismissRequest = { confirmarEliminacion = false },
            title = { Text("¿Eliminar recordatorio?") },
            text = { Text("Se eliminará de este dispositivo y, al sincronizar, también del servidor.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminacion = false
                    onEliminar()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminacion = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DetalleContenido(
    recordatorio: Recordatorio,
    procesando: Boolean,
    onAvanzarEstado: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        if (recordatorio.estadoSync != EstadoSync.SINCRONIZADO) {
            EtiquetaSincronizacion(recordatorio.estadoSync)
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
        FilaDato("Fecha límite", recordatorio.fechaLimite ?: "Sin fecha")
        FilaDato("Ubicación", recordatorio.ubicacion ?: "Sin ubicación")
        FilaDato("Aviso", recordatorio.aviso ?: "Sin aviso")
        FilaDato("Sincronización", recordatorio.estadoSync.etiqueta())
        FilaDato(
            "Última modificación",
            if (recordatorio.actualizadoEn > 0)
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(recordatorio.actualizadoEn))
            else "Sin datos"
        )
        FilaDato("Identificador", recordatorio.id)

        // Acción principal: avanzar el estado. Se guarda en Room aunque no haya conexión.
        Button(
            onClick = onAvanzarEstado,
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when (estado) {
                    EstadoRecordatorio.Pendiente -> "Marcar en curso"
                    EstadoRecordatorio.EnCurso -> "Marcar completado"
                    is EstadoRecordatorio.Completado -> "Reabrir"
                }
            )
        }
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

@Preview(showBackground = true, name = "Detalle · Pendiente de sincronizar")
@Composable
private fun PreviewDetalle() {
    RecordatoriosTheme {
        DetalleScreen(
            uiState = DetalleUiState.Contenido(
                DatosSimulados.recordatorios.first().copy(estadoSync = EstadoSync.PENDIENTE)
            ),
            onVolver = {}, onReintentar = {}, onEditar = {}, onAvanzarEstado = {},
            onEliminar = {}, onErrorMostrado = {}
        )
    }
}
