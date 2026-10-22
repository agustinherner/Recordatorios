# Recordatorios

App Android de recordatorios desarrollada en **Kotlin** con **Jetpack Compose** y **Material 3**.
Actividad 1 de la materia **Programación Mobile** (Tecnicatura Superior en Programación, Teclab).

Es la primera versión de una app de recordatorios contextuales. Modela recordatorios con categoría, prioridad y estado, valida los datos antes de mostrarlos y presenta una lista de ejemplo que se puede filtrar por estado.

## Funcionalidades

- Lista de recordatorios con título, categoría (con su color), prioridad y estado.
- Datos opcionales: descripción, fecha límite, ubicación y aviso.
- Filtros por estado: Todos, Pendientes, En curso y Completados.
- El filtro elegido se mantiene al girar la pantalla.
- Validación de datos: los registros inválidos se descartan y se informan en Logcat (etiqueta `Recordatorios`) sin cerrar la app.

## Estructura

```
app/src/main/java/com/example/recordatorios/
├── MainActivity.kt              Punto de entrada; aloja la interfaz Compose
├── domain/
│   ├── Modelos.kt               Data classes, sealed classes y funciones de extensión
│   └── Validaciones.kt          Reglas de validación (título, descripción, fecha)
├── data/
│   └── DatosSimulados.kt        Datos de ejemplo, List / Set / Map y filtros
└── ui/
    ├── PantallaRecordatorios.kt Pantalla inicial y previews
    └── theme/                   Tema Material 3
```

## Conceptos aplicados

| Concepto | Dónde |
| --- | --- |
| `data class` e inmutabilidad (`val`, `copy()`) | `Recordatorio`, `Categoria` |
| `sealed class` / `sealed interface` y `when` exhaustivo | `Prioridad`, `EstadoRecordatorio`, `EstadoPantalla` |
| Null safety (`?`, `?.`, `?:`, `?.let`) | Datos opcionales del recordatorio |
| Colecciones `List`, `Set`, `Map` | `DatosSimulados.kt` |
| `filter`, `map`, `mapNotNull`, `groupingBy` | Filtros y transformaciones |
| Validación sin excepciones (`toIntOrNull`, `Result`) | `Validaciones.kt` |
| Compose: estado, `rememberSaveable`, `LazyColumn`, `@Preview` | `PantallaRecordatorios.kt` |
| Recursos centralizados | `res/values/strings.xml` |

## Cómo ejecutarla

1. Clonar el repositorio:
   ```
   git clone https://github.com/agustinherner/Recordatorios.git
   ```
2. Abrir la carpeta en **Android Studio** y esperar la sincronización de Gradle.
3. Crear o elegir un emulador desde **Device Manager** (o conectar un dispositivo con depuración USB).
4. Presionar **Run ▶**.

## Requisitos

- Android Studio (probado en Rabbit 1)
- Android 7.0 o superior (`minSdk 24`), `targetSdk 37`

## Autor

Agustín Ignacio Herner
