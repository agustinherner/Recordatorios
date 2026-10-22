package com.example.recordatorios

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.domain.EstadoPantalla
import com.example.recordatorios.ui.PantallaRecordatorios
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * Única Activity de la app: punto de entrada y CONTENEDOR de la interfaz
 * Compose (Módulo 1 · U1 · Tema 3, "Activity, contexto y ciclo de vida").
 * No contiene reglas ni datos: solo arma el estado inicial y lo entrega a la pantalla.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Registro en Logcat de los datos simulados descartados por validación.
        // Filtrá Logcat por la etiqueta "Recordatorios" para verlo (sirve como evidencia de diagnóstico).
        DatosSimulados.erroresDeCarga.forEach { Log.w(TAG, "Recordatorio descartado: $it") }
        Log.i(TAG, "Recordatorios válidos cargados: ${DatosSimulados.recordatorios.size}")

        // Estado inicial de la pantalla: con datos o vacío, según la lista.
        val estadoInicial = if (DatosSimulados.recordatorios.isEmpty()) {
            EstadoPantalla.Vacio
        } else {
            EstadoPantalla.ConDatos(DatosSimulados.recordatorios)
        }

        setContent {
            RecordatoriosTheme {
                PantallaRecordatorios(estado = estadoInicial)
            }
        }
    }

    companion object {
        private const val TAG = "Recordatorios"
    }
}
