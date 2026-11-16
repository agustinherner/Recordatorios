package com.example.recordatorios.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.ContenedorDependencias
import com.example.recordatorios.domain.FiltroPrioridad
import com.example.recordatorios.domain.OrdenListado
import com.example.recordatorios.domain.PreferenciasUsuario
import com.example.recordatorios.domain.TemaApp
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/** Destino de Ajustes: conecta el ViewModel con la pantalla. */
@Composable
fun AjustesDestino(
    onVolver: () -> Unit,
    viewModel: AjustesViewModel = viewModel { AjustesViewModel(ContenedorDependencias.preferencias) }
) {
    // Observación consciente del ciclo de vida del StateFlow que sale de DataStore.
    val preferencias by viewModel.preferencias.collectAsStateWithLifecycle()
    AjustesScreen(
        preferencias = preferencias,
        onVolver = onVolver,
        onTemaChange = viewModel::onTemaChange,
        onMostrarCompletadosChange = viewModel::onMostrarCompletadosChange,
        onOrdenChange = viewModel::onOrdenChange,
        onFiltroPrioridadChange = viewModel::onFiltroPrioridadChange,
        onRestablecer = viewModel::onRestablecer
    )
}

/** Pantalla de preferencias: muestra el modelo y emite eventos; no conoce DataStore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    preferencias: PreferenciasUsuario,
    onVolver: () -> Unit,
    onTemaChange: (TemaApp) -> Unit,
    onMostrarCompletadosChange: (Boolean) -> Unit,
    onOrdenChange: (OrdenListado) -> Unit,
    onFiltroPrioridadChange: (FiltroPrioridad) -> Unit,
    onRestablecer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Estas opciones se guardan en el dispositivo y se conservan al cerrar la app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Seccion("Tema") {
                GrupoOpciones(TemaApp.entries, preferencias.tema, { it.etiqueta }, onTemaChange)
            }
            HorizontalDivider()

            // Toda la fila responde al toque (toggleable con rol Switch) para accesibilidad.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferencias.mostrarCompletados,
                        onValueChange = onMostrarCompletadosChange,
                        role = Role.Switch
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Mostrar completados", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Si se desactiva, el listado oculta los recordatorios completados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = preferencias.mostrarCompletados, onCheckedChange = null)
            }
            HorizontalDivider()

            Seccion("Ordenar listado por") {
                GrupoOpciones(OrdenListado.entries, preferencias.orden, { it.etiqueta }, onOrdenChange)
            }
            HorizontalDivider()

            Seccion("Filtrar por prioridad") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiltroPrioridad.entries.forEach { filtro ->
                        FilterChip(
                            selected = filtro == preferencias.filtroPrioridad,
                            onClick = { onFiltroPrioridadChange(filtro) },
                            label = { Text(filtro.etiqueta) }
                        )
                    }
                }
            }
            HorizontalDivider()

            OutlinedButton(onClick = onRestablecer, modifier = Modifier.fillMaxWidth()) {
                Text("Restablecer valores predeterminados")
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        contenido()
    }
}

/** Opciones excluyentes con RadioButton; cada fila completa es seleccionable. */
@Composable
private fun <T> GrupoOpciones(
    opciones: List<T>,
    seleccionada: T,
    etiqueta: (T) -> String,
    onSeleccion: (T) -> Unit
) {
    Column(Modifier.selectableGroup()) {
        opciones.forEach { opcion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = opcion == seleccionada,
                        onClick = { onSeleccion(opcion) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = opcion == seleccionada, onClick = null)
                Text(etiqueta(opcion), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "Ajustes")
@Composable
private fun PreviewAjustes() {
    RecordatoriosTheme {
        AjustesScreen(PreferenciasUsuario(), {}, {}, {}, {}, {}, {})
    }
}
