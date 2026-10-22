package com.example.recordatorios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.R
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.filtrarPorEstado
import com.example.recordatorios.data.ordenadosParaMostrar
import com.example.recordatorios.data.soloPendientes
import com.example.recordatorios.domain.EstadoPantalla
import com.example.recordatorios.domain.EstadoRecordatorio
import com.example.recordatorios.domain.Prioridad
import com.example.recordatorios.domain.Recordatorio
import com.example.recordatorios.domain.estaPendiente
import com.example.recordatorios.domain.etiqueta
import com.example.recordatorios.ui.theme.RecordatoriosTheme

// ---------------------------------------------------------------------------
// PANTALLA INICIAL (Módulo 1 · U2 · Tema 4: Jetpack Compose y Material 3).
// La interfaz se DESCRIBE a partir del estado: no se busca ni se modifica
// ningún componente a mano. Cuando cambia el filtro elegido, Compose
// recompone y la lista se vuelve a calcular.
// ---------------------------------------------------------------------------

/** Opciones del filtro. Enum (y no sealed) porque no llevan datos y es Serializable → rememberSaveable lo guarda. */
enum class FiltroEstado(val texto: String) { TODOS("Todos"), PENDIENTES("Pendientes"), EN_CURSO("En curso"), COMPLETADOS("Completados") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRecordatorios(
    estado: EstadoPantalla,
    modifier: Modifier = Modifier
) {
    // Estado simple de la pantalla. rememberSaveable (y no remember) para
    // que el filtro elegido sobreviva a la recreación (girar el teléfono).
    var filtro by rememberSaveable { mutableStateOf(FiltroEstado.TODOS) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.titulo_pantalla)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingInterno ->
        // when EXHAUSTIVO sobre la sealed interface: si mañana se agrega un
        // estado nuevo, el compilador marca este bloque como incompleto.
        when (estado) {
            EstadoPantalla.Cargando -> MensajeCentrado(paddingInterno) { CircularProgressIndicator() }
            EstadoPantalla.Vacio -> MensajeCentrado(paddingInterno) {
                Text(stringResource(R.string.sin_recordatorios), style = MaterialTheme.typography.bodyLarge)
            }
            is EstadoPantalla.Error -> MensajeCentrado(paddingInterno) {
                Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
            }
            is EstadoPantalla.ConDatos -> {
                // Lista visible DERIVADA del estado (filtro + datos). No se guarda aparte.
                val visibles = when (filtro) {
                    FiltroEstado.TODOS -> estado.recordatorios
                    FiltroEstado.PENDIENTES -> estado.recordatorios.filtrarPorEstado(EstadoRecordatorio.Pendiente)
                    FiltroEstado.EN_CURSO -> estado.recordatorios.filtrarPorEstado(EstadoRecordatorio.EnCurso)
                    FiltroEstado.COMPLETADOS -> estado.recordatorios.filtrarPorEstado(EstadoRecordatorio.Completado(""))
                }.ordenadosParaMostrar()

                Column(Modifier.padding(paddingInterno).fillMaxSize()) {
                    Resumen(
                        total = estado.recordatorios.size,
                        pendientes = estado.recordatorios.soloPendientes().size,
                        // Set calculado sobre los datos que RECIBE la pantalla (no sobre el objeto global)
                        categorias = estado.recordatorios.map { it.categoria }.toSet().size
                    )
                    FiltrosEstado(seleccionado = filtro, onSeleccion = { filtro = it })
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibles, key = { it.id }) { recordatorio ->
                            TarjetaRecordatorio(recordatorio)
                        }
                    }
                }
            }
        }
    }
}

/** Contenedor genérico para los estados Cargando / Vacío / Error. El contenido llega como lambda @Composable. */
@Composable
private fun MensajeCentrado(padding: PaddingValues, contenido: @Composable () -> Unit) {
    Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun Resumen(total: Int, pendientes: Int, categorias: Int) {
    Text(
        text = stringResource(R.string.resumen, total, pendientes, categorias),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosEstado(seleccionado: FiltroEstado, onSeleccion: (FiltroEstado) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroEstado.entries.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionado,
                onClick = { onSeleccion(opcion) },   // la lambda comunica el evento hacia quien guarda el estado
                label = { Text(opcion.texto) }
            )
        }
    }
}

/** Tarjeta de un recordatorio: título, categoría, prioridad y estado (lo que pide la consigna) + datos opcionales. */
@Composable
fun TarjetaRecordatorio(recordatorio: Recordatorio, modifier: Modifier = Modifier) {
    // El color sale del Map categoría → color (Long ARGB) y se convierte a Color de Compose.
    val colorCategoria = Color(DatosSimulados.colorDe(recordatorio.categoria))
    val completado = !recordatorio.estaPendiente()

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // IntrinsicSize.Min: la fila toma la altura de su contenido, así la franja de color lo acompaña.
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja lateral con el color de la categoría.
            Box(Modifier.width(6.dp).fillMaxHeight().background(colorCategoria))
            Column(Modifier.padding(12.dp).weight(1f)) {
                Text(
                    text = recordatorio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (completado) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta(
                        texto = "${DatosSimulados.etiquetaDe(recordatorio.categoria)} · ${recordatorio.categoria.nombre}",
                        color = colorCategoria
                    )
                    Etiqueta(texto = "Prioridad ${recordatorio.prioridad.etiqueta}", color = colorDePrioridad(recordatorio.prioridad))
                    Text(
                        text = recordatorio.estado.etiqueta(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Datos OPCIONALES: ?.let dibuja la línea solo si el valor existe (null safety).
                recordatorio.descripcion?.let { DetalleOpcional(it) }
                recordatorio.fechaLimite?.let { DetalleOpcional(stringResource(R.string.vence, it)) }
                recordatorio.ubicacion?.let { DetalleOpcional(stringResource(R.string.ubicacion, it)) }
                recordatorio.aviso?.let { DetalleOpcional(stringResource(R.string.aviso, it)) }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))   // primero recorte y fondo, después relleno: el orden importa
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun DetalleOpcional(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/** when exhaustivo sobre la sealed class Prioridad: no hace falta else. */
@Composable
private fun colorDePrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.Alta -> MaterialTheme.colorScheme.error
    Prioridad.Media -> Color(0xFFF9A825)
    Prioridad.Baja -> MaterialTheme.colorScheme.tertiary
}

// ---------------------------------------------------------------------------
// PREVIEWS: permiten ver la pantalla en Android Studio sin ejecutar la app.
// No reemplazan la ejecución en emulador/dispositivo (consigna "Ejecuta").
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Con datos")
@Composable
private fun PreviewConDatos() {
    RecordatoriosTheme { PantallaRecordatorios(EstadoPantalla.ConDatos(DatosSimulados.recordatorios)) }
}

@Preview(showBackground = true, name = "Vacío")
@Composable
private fun PreviewVacio() {
    RecordatoriosTheme { PantallaRecordatorios(EstadoPantalla.Vacio) }
}

@Preview(showBackground = true, name = "Tarjeta")
@Composable
private fun PreviewTarjeta() {
    RecordatoriosTheme { TarjetaRecordatorio(DatosSimulados.recordatorios.first(), Modifier.padding(16.dp)) }
}
