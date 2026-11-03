package com.example.recordatorios

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.ui.navegacion.NavegacionRecordatorios
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * Única Activity de la app: contenedor de la interfaz Compose.
 * No tiene estado ni lógica de pantallas: eso vive en los ViewModel. Acá solo
 * se aplica el tema y se monta el grafo de navegación.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Diagnóstico de la Actividad 1: datos simulados descartados por validación (Logcat, etiqueta "Recordatorios").
        DatosSimulados.erroresDeCarga.forEach { Log.w(TAG, "Recordatorio descartado: $it") }

        setContent {
            RecordatoriosTheme {
                NavegacionRecordatorios()
            }
        }
    }

    companion object {
        private const val TAG = "Recordatorios"
    }
}
