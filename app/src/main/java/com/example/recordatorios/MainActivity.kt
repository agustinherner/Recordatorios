package com.example.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.data.ContenedorDependencias
import com.example.recordatorios.domain.PreferenciasUsuario
import com.example.recordatorios.domain.TemaApp
import com.example.recordatorios.ui.navegacion.NavegacionRecordatorios
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/**
 * Única Activity de la app: contenedor de la interfaz Compose.
 *
 * Actividad 3: el tema claro/oscuro sale de la preferencia guardada en
 * DataStore. Se observa con collectAsStateWithLifecycle; cuando la persona
 * cambia el tema en Ajustes, DataStore emite y toda la interfaz se recompone.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferencias by ContenedorDependencias.preferencias.preferencias
                .collectAsStateWithLifecycle(initialValue = PreferenciasUsuario())

            val oscuro = when (preferencias.tema) {
                TemaApp.SISTEMA -> isSystemInDarkTheme()
                TemaApp.CLARO -> false
                TemaApp.OSCURO -> true
            }

            RecordatoriosTheme(darkTheme = oscuro) {
                NavegacionRecordatorios()
            }
        }
    }
}
