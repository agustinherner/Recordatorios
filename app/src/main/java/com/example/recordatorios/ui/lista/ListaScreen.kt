package com.example.recordatorios.ui.lista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.R
import com.example.recordatorios.data.ContenedorDependencias
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.filtrarPorEstado
import com.example.recordatorios.data.soloPendientes
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.ui.componentes.PanelEstado
import com.example.recordatorios.ui.componentes.RecordatorioCard
import com.example.recordatorios.ui.componentes.TipoPanel
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * Destino del listado: obtiene el ViewModel, observa su estado y conecta
 * eventos con lambdas. La pantalla de abajo (ListaScreen) no sabe nada de esto.
 */
@Composable
fun ListaDestino(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    viewModel: ListaViewModel = viewModel {
        ListaViewModel(ContenedorDependencias.repositorio, ContenedorDependencias.simulacion)
    }
) {
    // Recolección consciente del ciclo de vida: se detiene cuando la pantalla no está activa.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ListaScreen(
        uiState = uiState,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onReintentar = viewModel::onReintentar,
        onSimular = viewModel::onSimular
    )
}

/** Filtro visual por estado (de la Actividad 1). Es estado simple de UI → rememberSaveable. */
enum class FiltroEstado(val texto: String) {
    TODOS("Todos"), PENDIENTES("Pendientes"), EN_CURSO("En curso"), COMPLETADOS("Completados")
}

/** Pantalla de listado SIN estado propio de negocio: muestra el UiState y emite eventos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaScreen(
    uiState: ListaUiState,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onReintentar: () -> Unit,
    onSimular: (OpcionSimulacion) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.titulo_pantalla)) },
                actions = { MenuSimulacion(onSimular) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCrearRecordatorio,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo") }
            )
        }
    ) { innerPadding ->
        // innerPadding de Scaffold: evita que la barra superior tape el contenido.
        val contenidoModifier = Modifier.padding(innerPadding)

        // when EXHAUSTIVO sobre el UiState: siempre se muestra uno solo de los estados.
        when (uiState) {
            ListaUiState.Cargando -> PanelEstado(
                tipo = TipoPanel.Carga,
                mensaje = "Cargando recordatorios…",
                modifier = contenidoModifier
            )
            ListaUiState.Vacio -> PanelEstado(
                tipo = TipoPanel.Vacio,
                mensaje = "Todavía no tenés recordatorios.\nCreá el primero para empezar.",
                modifier = contenidoModifier,
                textoAccion = "Crear recordatorio",
                onAccion = onCrearRecordatorio
            )
            is ListaUiState.Error -> PanelEstado(
                tipo = TipoPanel.Error,
                mensaje = uiState.mensaje,
                modifier = contenidoModifier,
                textoAccion = "Reintentar",
                onAccion = onReintentar
            )
            is ListaUiState.Contenido -> ListaContenido(
                recordatorios = uiState.recordatorios,
                onRecordatorioClick = onRecordatorioClick,
                modifier = contenidoModifier
            )
        }
    }
}

@Composable
private fun ListaContenido(
    recordatorios: List<Recordatorio>,
    onRecordatorioClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var filtro by rememberSaveable { mutableStateOf(FiltroEstado.TODOS) }

    // Lista visible DERIVADA de los datos + el filtro: no se guarda como otro estado.
    val visibles = when (filtro) {
        FiltroEstado.TODOS -> recordatorios
        FiltroEstado.PENDIENTES -> recordatorios.filtrarPorEstado(EstadoRecordatorio.Pendiente)
        FiltroEstado.EN_CURSO -> recordatorios.filtrarPorEstado(EstadoRecordatorio.EnCurso)
        FiltroEstado.COMPLETADOS -> recordatorios.filtrarPorEstado(EstadoRecordatorio.Completado(""))
    }

    Column(modifier.fillMaxSize()) {
        Text(
            text = stringResource(
                R.string.resumen,
                recordatorios.size,
                recordatorios.soloPendientes().size,
                recordatorios.map { it.categoria }.toSet().size
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )
        // LazyRow para los filtros desplazables.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FiltroEstado.entries, key = { it.name }) { opcion ->
                FilterChip(
                    selected = opcion == filtro,
                    onClick = { filtro = opcion },
                    label = { Text(opcion.texto) }
                )
            }
        }

        if (visibles.isEmpty()) {
            // Vacío por filtro: no es un error, se ofrece una acción relacionada.
            PanelEstado(
                tipo = TipoPanel.Vacio,
                mensaje = "Ningún recordatorio coincide con el filtro.",
                textoAccion = "Ver todos",
                onAccion = { filtro = FiltroEstado.TODOS }
            )
        } else {
            LazyColumn(
                // abajo deja lugar para que el botón flotante no tape la última tarjeta
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // key estable = id del recordatorio (no el índice): si la lista se
                // reordena o se agrega un registro, cada tarjeta conserva su identidad.
                items(visibles, key = { it.id }) { recordatorio ->
                    RecordatorioCard(
                        recordatorio = recordatorio,
                        onClick = { onRecordatorioClick(recordatorio.id) }   // avisa el id, no navega
                    )
                }
            }
        }
    }
}

/** Menú ⋮ de la barra superior con las opciones para simular estados. */
@Composable
private fun MenuSimulacion(onSimular: (OpcionSimulacion) -> Unit) {
    var abierto by remember { mutableStateOf(false) }   // estado transitorio y local → remember
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de simulación")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            OpcionSimulacion.entries.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.texto) },
                    onClick = {
                        abierto = false
                        onSimular(opcion)
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PREVIEWS: la pantalla recibe el estado, así que se puede ver cada caso
// sin ViewModel ni repositorio.
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Listado · Con datos")
@Composable
private fun PreviewListaContenido() {
    RecordatoriosTheme {
        ListaScreen(ListaUiState.Contenido(DatosSimulados.recordatorios), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Listado · Vacío")
@Composable
private fun PreviewListaVacia() {
    RecordatoriosTheme { ListaScreen(ListaUiState.Vacio, {}, {}, {}, {}) }
}

@Preview(showBackground = true, name = "Listado · Error")
@Composable
private fun PreviewListaError() {
    RecordatoriosTheme {
        ListaScreen(ListaUiState.Error("No pudimos cargar los recordatorios."), {}, {}, {}, {})
    }
}
