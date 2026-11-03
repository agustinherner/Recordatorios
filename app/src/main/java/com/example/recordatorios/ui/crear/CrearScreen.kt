package com.example.recordatorios.ui.crear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.ContenedorDependencias
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.LARGO_MAXIMO_DESCRIPCION
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.ui.componentes.CampoTexto
import com.example.recordatorios.ui.componentes.SelectorCategoria
import com.example.recordatorios.ui.componentes.SelectorPrioridad
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/** Destino de creación: conecta el ViewModel con la pantalla y procesa el evento de éxito. */
@Composable
fun CrearDestino(
    onGuardado: () -> Unit,
    onCancelar: () -> Unit,
    viewModel: CrearViewModel = viewModel { CrearViewModel(ContenedorDependencias.repositorio) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Evento de una sola vez: cuando el guardado termina con éxito se vuelve
    // al listado y el estado se restablece (onExitoProcesado).
    val envio = uiState.envio
    LaunchedEffect(envio) {
        if (envio is EstadoEnvio.Exito) {
            onGuardado()
            viewModel.onExitoProcesado()
        }
    }

    CrearScreen(
        uiState = uiState,
        onTituloChange = viewModel::onTituloChange,
        onDescripcionChange = viewModel::onDescripcionChange,
        onCategoriaSeleccionada = viewModel::onCategoriaSeleccionada,
        onPrioridadSeleccionada = viewModel::onPrioridadSeleccionada,
        onGuardar = viewModel::onGuardar,
        onCancelar = onCancelar
    )
}

/** Formulario de creación: solo muestra el estado y emite eventos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearScreen(
    uiState: CrearUiState,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaSeleccionada: (Categoria) -> Unit,
    onPrioridadSeleccionada: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val editable = !uiState.enviando   // durante el envío los campos quedan bloqueados

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo recordatorio") },
                navigationIcon = {
                    IconButton(onClick = onCancelar, enabled = editable) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar y volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()                        // el teclado no tapa los campos
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CampoTexto(
                valor = uiState.titulo,
                onValorChange = onTituloChange,
                etiqueta = "Título *",
                mensajeError = uiState.errorTitulo,
                textoAyuda = "Mínimo 4 caracteres",
                habilitado = editable,
                accionTeclado = ImeAction.Next,
                onAccionTeclado = { focusManager.moveFocus(FocusDirection.Down) }   // "Siguiente"
            )
            CampoTexto(
                valor = uiState.descripcion,
                onValorChange = onDescripcionChange,
                etiqueta = "Descripción (opcional)",
                mensajeError = uiState.errorDescripcion?.let {
                    "$it (${uiState.descripcion.trim().length}/$LARGO_MAXIMO_DESCRIPCION)"
                },
                textoAyuda = "${uiState.descripcion.trim().length}/$LARGO_MAXIMO_DESCRIPCION",
                habilitado = editable,
                unaLinea = false,
                lineasMinimas = 3,
                accionTeclado = ImeAction.Done,
                onAccionTeclado = { focusManager.clearFocus() }                    // "Listo" cierra el teclado
            )
            SelectorCategoria(
                categorias = uiState.categoriasDisponibles,
                seleccionada = uiState.categoria,
                onSeleccion = onCategoriaSeleccionada,
                habilitado = editable,
                textoAyuda = uiState.errorCategoria
            )
            SelectorPrioridad(
                opciones = uiState.prioridadesDisponibles,
                seleccionada = uiState.prioridad,
                onSeleccion = onPrioridadSeleccionada,
                habilitado = editable,
                textoAyuda = uiState.errorPrioridad
            )

            // Error general de guardado: cerca de la acción y con opción de reintento.
            val envio = uiState.envio
            if (envio is EstadoEnvio.Error) {
                ErrorGuardado(mensaje = envio.mensaje, onReintentar = onGuardar)
            }

            // Explica por qué el botón está deshabilitado (no depende solo del color gris).
            if (!uiState.formularioValido) {
                Text(
                    text = "Para guardar falta: ${uiState.camposPendientes.joinToString(", ")}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onCancelar, enabled = editable) { Text("Cancelar") }
                // Deshabilitado si el formulario es inválido o hay un envío en curso.
                Button(onClick = onGuardar, enabled = uiState.puedeGuardar) {
                    if (uiState.enviando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando…")
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorGuardado(mensaje: String, onReintentar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive }   // se anuncia apenas aparece
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onReintentar) { Text("Reintentar") }
        }
    }
}

@Preview(showBackground = true, name = "Formulario · Con error de validación")
@Composable
private fun PreviewCrearInvalido() {
    RecordatoriosTheme {
        CrearScreen(
            uiState = CrearUiState(
                titulo = "Ok", tituloTocado = true,
                categoriasDisponibles = DatosSimulados.categoriasDisponibles
            ),
            onTituloChange = {}, onDescripcionChange = {}, onCategoriaSeleccionada = {},
            onPrioridadSeleccionada = {}, onGuardar = {}, onCancelar = {}
        )
    }
}

@Preview(showBackground = true, name = "Formulario · Enviando")
@Composable
private fun PreviewCrearEnviando() {
    RecordatoriosTheme {
        CrearScreen(
            uiState = CrearUiState(
                titulo = "Llamar al contador",
                categoria = DatosSimulados.TRABAJO,
                prioridad = Prioridad.Alta,
                categoriasDisponibles = DatosSimulados.categoriasDisponibles,
                envio = EstadoEnvio.Enviando
            ),
            onTituloChange = {}, onDescripcionChange = {}, onCategoriaSeleccionada = {},
            onPrioridadSeleccionada = {}, onGuardar = {}, onCancelar = {}
        )
    }
}
