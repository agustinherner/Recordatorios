package com.example.recordatorios.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.recordatorios.domain.Categoria
import com.example.recordatorios.domain.Prioridad

/**
 * Campo de texto CONTROLADO: no guarda el texto, lo recibe en [valor] y
 * comunica cada cambio con [onValorChange]. El estado vive en el ViewModel.
 *
 * El error se comunica de tres formas, no solo con color:
 * isError (borde), un mensaje textual (supportingText) y la propiedad
 * semántica error() para los lectores de pantalla.
 */
@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    mensajeError: String? = null,
    textoAyuda: String? = null,
    habilitado: Boolean = true,
    unaLinea: Boolean = true,
    lineasMinimas: Int = 1,
    accionTeclado: ImeAction = ImeAction.Next,
    onAccionTeclado: () -> Unit = {}
) {
    val textoInferior = mensajeError ?: textoAyuda
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        isError = mensajeError != null,
        supportingText = if (textoInferior != null) { { Text(textoInferior) } } else null,
        enabled = habilitado,
        singleLine = unaLinea,
        minLines = lineasMinimas,
        // El teclado orienta (mayúscula inicial, botón Siguiente/Listo), pero no reemplaza la validación.
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = accionTeclado
        ),
        keyboardActions = KeyboardActions(onAny = { onAccionTeclado() }),
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (mensajeError != null) error(mensajeError) }
    )
}

/**
 * Selector de categoría con chips desplazables (LazyRow). Solo ofrece las
 * opciones definidas; igual el ViewModel vuelve a validar la elección.
 */
@Composable
fun SelectorCategoria(
    categorias: List<Categoria>,
    seleccionada: Categoria?,
    onSeleccion: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    textoAyuda: String? = null
) {
    Column(modifier) {
        Text("Categoría *", style = MaterialTheme.typography.titleSmall)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categorias, key = { it.nombre }) { categoria ->
                FilterChip(
                    selected = categoria == seleccionada,
                    onClick = { onSeleccion(categoria) },
                    label = { Text(categoria.nombre) },
                    enabled = habilitado
                )
            }
        }
        textoAyuda?.let { TextoAyuda(it) }
    }
}

/**
 * Selector de prioridad con RadioButton (opciones excluyentes).
 * selectableGroup + selectable(role = RadioButton) hacen que TalkBack lea
 * "botón de selección, seleccionado / no seleccionado" y que toda la fila
 * (círculo + texto) responda al toque.
 */
@Composable
fun SelectorPrioridad(
    opciones: List<Prioridad>,
    seleccionada: Prioridad?,
    onSeleccion: (Prioridad) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    textoAyuda: String? = null
) {
    Column(modifier) {
        Text("Prioridad *", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            opciones.forEach { prioridad ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = prioridad == seleccionada,
                            onClick = { onSeleccion(prioridad) },
                            enabled = habilitado,
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // onClick = null: el clic lo maneja la fila completa.
                    RadioButton(selected = prioridad == seleccionada, onClick = null, enabled = habilitado)
                    Text(prioridad.etiqueta, Modifier.padding(start = 4.dp))
                }
            }
        }
        textoAyuda?.let { TextoAyuda(it) }
    }
}

@Composable
private fun TextoAyuda(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp)
    )
}
