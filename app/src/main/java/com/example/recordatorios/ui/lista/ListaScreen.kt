package com.example.recordatorios.ui.lista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.recordatorios.data.soloPendientes
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.ui.componentes.BannerEstadoDatos
import com.example.recordatorios.ui.componentes.PanelEstado
import com.example.recordatorios.ui.componentes.RecordatorioCard
import com.example.recordatorios.ui.componentes.TipoPanel
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * Destino del listado: obtiene el ViewModel, observa su estado y conecta
 * eventos con lambdas. La pantalla de abajo (ListaScreen) no sabe nada de esto
 * ni accede al DAO: trabaja solo con el UiState.
 */
@Composable
fun ListaDestino(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit,
    onAbrirAjustes: () -> Unit,
    viewModel: ListaViewModel = viewModel {
        ListaViewModel(
            repositorio = ContenedorDependencias.repositorio,
            preferencias = ContenedorDependencias.preferencias,
            conectado = ContenedorDependencias.conectividad.conectado,
            simulacionLocal = ContenedorDependencias.simulacionLocal,
            controlServidor = ContenedorDependencias.controlServidor,
            servicio = ContenedorDependencias.descripcionServicio
        )
    }
) {
    // Recolección consciente del ciclo de vida: se detiene cuando la pantalla no está activa.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mensajeEvento by viewModel.mensajeEvento.collectAsStateWithLifecycle()

    ListaScreen(
        uiState = uiState,
        mensajeEvento = mensajeEvento,
        onMensajeMostrado = viewModel::onMensajeMostrado,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onAbrirAjustes = onAbrirAjustes,
        onSincronizar = viewModel::onSincronizar,
        onSimular = viewModel::onSimular
    )
}

/** Pantalla de listado SIN estado propio de negocio: muestra el UiState y emite eventos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaScreen(
    uiState: ListaUiState,
    mensajeEvento: String?,
    onMensajeMostrado: () -> Unit,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit,
    onAbrirAjustes: () -> Unit,
    onSincronizar: () -> Unit,
    onSimular: (OpcionSimulacion) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Evento de una sola vez: se muestra y se avisa al ViewModel que ya se consumió.
    LaunchedEffect(mensajeEvento) {
        if (mensajeEvento != null) {
            snackbarHostState.showSnackbar(mensajeEvento)
            onMensajeMostrado()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.titulo_pantalla)) },
                actions = {
                    IconButton(onClick = onSincronizar, enabled = !uiState.sincronizando) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sincronizar ahora")
                    }
                    IconButton(onClick = onAbrirAjustes) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                    MenuSimulacion(onSimular)
                },
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
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            // Barra de progreso fina mientras sincroniza: no tapa el contenido.
            if (uiState.sincronizando) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            // Avisos de conectividad y sincronización: ACOMPAÑAN a la lista, no la reemplazan.
            BannerEstadoDatos(
                enLinea = uiState.enLinea,
                cambiosSinSincronizar = uiState.cambiosSinSincronizar,
                mensajeSincronizacion = uiState.mensajeSincronizacion,
                esError = uiState.mensajeSincronizacionEsError,
                onReintentar = onSincronizar
            )

            when {
                uiState.cargando -> PanelEstado(TipoPanel.Carga, "Cargando recordatorios…")

                uiState.errorLectura != null -> PanelEstado(
                    tipo = TipoPanel.Error,
                    mensaje = uiState.errorLectura
                )

                uiState.recordatorios.isEmpty() -> PanelEstado(
                    tipo = TipoPanel.Vacio,
                    mensaje = if (uiState.hayFiltrosActivos)
                        "Ningún recordatorio coincide con los filtros de Ajustes."
                    else "Todavía no tenés recordatorios.\nCreá el primero para empezar.",
                    textoAccion = if (uiState.hayFiltrosActivos) "Cambiar filtros" else "Crear recordatorio",
                    onAccion = if (uiState.hayFiltrosActivos) onAbrirAjustes else onCrearRecordatorio
                )

                else -> ListaContenido(
                    recordatorios = uiState.recordatorios,
                    resumenPreferencias = uiState.resumenPreferencias,
                    servicio = uiState.servicio,
                    onRecordatorioClick = onRecordatorioClick
                )
            }
        }
    }
}

@Composable
private fun ListaContenido(
    recordatorios: List<Recordatorio>,
    resumenPreferencias: String,
    servicio: String,
    onRecordatorioClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
        // Qué preferencias de DataStore están aplicadas y contra qué servicio se sincroniza.
        Text(
            text = "$resumenPreferencias · $servicio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyColumn(
            // abajo deja lugar para que el botón flotante no tape la última tarjeta
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // key estable = id (UUID): cada tarjeta conserva su identidad aunque la lista se reordene.
            items(recordatorios, key = { it.id }) { recordatorio ->
                RecordatorioCard(
                    recordatorio = recordatorio,
                    onClick = { onRecordatorioClick(recordatorio.id) }   // avisa el id, no navega
                )
            }
        }
    }
}

/** Menú ⋮ de la barra superior con las opciones para provocar errores y probar la app. */
@Composable
private fun MenuSimulacion(onSimular: (OpcionSimulacion) -> Unit) {
    var abierto by remember { mutableStateOf(false) }   // estado transitorio y local → remember
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de prueba")
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
// PREVIEWS: la pantalla recibe el estado, así que se puede ver cada caso sin
// ViewModel, Room ni red.
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Listado · Sin conexión con pendientes")
@Composable
private fun PreviewListaOffline() {
    RecordatoriosTheme {
        ListaScreen(
            uiState = ListaUiState(
                cargando = false,
                recordatorios = DatosSimulados.recordatorios,
                enLinea = false,
                cambiosSinSincronizar = 2,
                servicio = "Servidor simulado"
            ),
            mensajeEvento = null, onMensajeMostrado = {}, onCrearRecordatorio = {},
            onRecordatorioClick = {}, onAbrirAjustes = {}, onSincronizar = {}, onSimular = {}
        )
    }
}

@Preview(showBackground = true, name = "Listado · Vacío")
@Composable
private fun PreviewListaVacia() {
    RecordatoriosTheme {
        ListaScreen(
            uiState = ListaUiState(cargando = false),
            mensajeEvento = null, onMensajeMostrado = {}, onCrearRecordatorio = {},
            onRecordatorioClick = {}, onAbrirAjustes = {}, onSincronizar = {}, onSimular = {}
        )
    }
}
