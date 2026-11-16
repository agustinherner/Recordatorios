# Recordatorios

App Android de recordatorios desarrollada en **Kotlin** con **Jetpack Compose** y **Material 3**.
Materia **Programación Mobile** (Tecnicatura Superior en Programación, Teclab).

**Versión 3.0 — Actividad 3:** persistencia local con **DataStore** y **Room**, repositorio **local-first**,
servicio remoto con **Retrofit + OkHttp**, sincronización controlada con **WorkManager** y funcionamiento **offline**.

## Evolución

| Versión | Actividad | Qué agregó |
| --- | --- | --- |
| 1.0 | Actividad 1 | Modelos (data/sealed classes), validaciones, lista con filtros en Compose |
| 2.0 | Actividad 2 | Pantallas de listado, creación y detalle; ViewModel + StateFlow; repositorio en memoria; Navigation Compose |
| 3.0 | Actividad 3 | Room, DataStore, repositorio local-first, Retrofit/OkHttp, sincronización sin duplicados, edición y eliminación, Ajustes, offline |

## Funcionalidades

- Crear, consultar, **editar**, cambiar de estado y **eliminar** recordatorios, **con o sin conexión**.
- Los recordatorios se guardan en **Room** y siguen disponibles después de cerrar la app.
- **Ajustes** guardados con **DataStore**: tema (sistema/claro/oscuro), mostrar completados, orden del listado y filtro de prioridad.
- Sincronización con un servicio remoto: los cambios hechos sin conexión quedan **"Pendiente de sincronizar"** y se envían solos cuando vuelve la red.
- Avisos de **sin conexión**, cambios pendientes, sincronización en curso y errores, sin ocultar los datos guardados.
- Menú **⋮ de pruebas** para provocar errores 500, 400, timeout, conflictos, falta de espacio y preferencias inválidas.

## Arquitectura de la capa de datos

```
Compose ──evento──► ViewModel ──► RecordatorioRepository (local-first)
   ▲                                   │          │            │
   │ StateFlow                 DataStore (prefs)  Room (DAO)  FuenteRemota (Retrofit/OkHttp)
   └──── ViewModel ◄── Flow ◄──────────────────────┘            ▲
                                                                │
                                    WorkManager (trabajo único) ─┘ sincronizar()
```

- **Room es la fuente de verdad** de la interfaz. La red nunca alimenta la pantalla directamente:
  lo que llega del servidor se mapea (DTO → Entity) y se guarda en Room; la pantalla se entera por `Flow`.
- **Modelos separados por capa:** `RecordatorioDto` (servicio) · `RecordatorioEntity` (Room) · `Recordatorio` (dominio/UI), con mapeos explícitos en `data/Mapeos.kt`.
- **Sin duplicados:** id estable (UUID generado en el dispositivo), `PUT` idempotente, trabajo único de WorkManager (`KEEP`) y `Mutex` en el repositorio.

## Estructura

```
app/src/main/java/com/example/recordatorios/
├── RecordatoriosApp.kt            Application: contenedor y disparadores de sincronización
├── MainActivity.kt                Tema según DataStore + navegación
├── domain/                        Modelo puro (sin Android): Recordatorio, EstadoSync, Preferencias, validaciones
├── data/
│   ├── ContenedorDependencias.kt  Instancias únicas (Room, DataStore, Retrofit, repositorio)
│   ├── RecordatorioRepository.kt  Contrato + ResultadoSincronizacion + EstadoSincronizacion
│   ├── RecordatorioRepositoryLocalFirst.kt  Implementación local-first y sincronización
│   ├── Mapeos.kt                  DTO ↔ Entity ↔ Dominio
│   ├── ErroresAlmacenamiento.kt   Clasificación de errores locales y mensajes
│   ├── DatosSimulados.kt          Datos de ejemplo (contenido inicial del servidor simulado)
│   ├── local/                     Room: RecordatorioEntity, RecordatorioDao, AppDatabase, Conversores
│   ├── preferencias/              DataStore: instancia única + PreferenciasRepository
│   ├── remoto/                    Retrofit: DTO, API, FuenteRemota, ResultadoRemoto, ServidorSimulado
│   ├── sincronizacion/            WorkManager (Worker + Programador) y resolución de conflictos
│   └── conectividad/              ObservadorConectividad (ConnectivityManager → Flow)
└── ui/                            lista · crear (crear/editar) · detalle · ajustes · componentes · navegacion
app/src/test/                      Tests locales: mapeos, conflictos, preferencias y orden
servidor-json/                     Servicio REST opcional con json-server
remote.properties.example          Ejemplo de configuración del servicio (sin secretos)
```

## Configurar, compilar y ejecutar

### Requisitos
- Android Studio (probado en la versión Rabbit) con el SDK de Android 37.
- Emulador o dispositivo con Android 7.0 o superior (`minSdk 24`).

### 1. Abrir el proyecto
1. Abrir la carpeta del proyecto en **Android Studio**.
2. Esperar la sincronización de Gradle (descarga Room, DataStore, Retrofit, OkHttp y WorkManager).

### 2. Elegir el servicio remoto (opcional)
Sin configurar nada, la app usa el **servidor simulado** incluido (no necesita internet ni instalar nada).

Para usar un servicio HTTP real:
1. Copiar `remote.properties.example` como `remote.properties` (misma carpeta). Ese archivo **no** se sube a Git.
2. Editar los valores:
   ```properties
   API_SIMULADA=false
   API_BASE_URL=http://10.0.2.2:3000/
   ```
3. **File › Sync Project with Gradle Files**.

#### Opción json-server (servicio real en la PC)
Requiere [Node.js](https://nodejs.org/).
```
cd servidor-json
npm install
npm start
```
El servicio queda en `http://localhost:3000/recordatorios`. Desde el emulador se accede con `http://10.0.2.2:3000/`.
Para volver a los datos iniciales, copiar `db.inicial.json` sobre `db.json`.

### 3. Ejecutar
1. Elegir un emulador en **Device Manager** (o conectar un dispositivo con depuración USB).
2. Presionar **Run ▶**.

### 4. Tests locales
```
gradlew test        (Windows: gradlew.bat test)
```
o clic derecho sobre `app/src/test` › **Run Tests**.

### Errores frecuentes
| Problema | Solución |
| --- | --- |
| `Plugin [id: 'com.google.devtools.ksp'] was not found` o error de compatibilidad de KSP | En `gradle/libs.versions.toml`, cambiar `ksp` por la versión 2.3.x más reciente que sugiera Android Studio. |
| `CLEARTEXT communication not permitted` | Solo se permite HTTP para `10.0.2.2` y `localhost` (`res/xml/network_security_config.xml`). Otros servidores deben usar HTTPS. |
| La app no se conecta a json-server | Verificar que `npm start` esté corriendo y que la URL termine en `/`. Con un celular físico, usar la IP de la PC en lugar de `10.0.2.2`. |
| Cambié `remote.properties` y no se aplica | Sincronizar Gradle y volver a ejecutar (los valores se compilan en `BuildConfig`). |

## Cómo probar el funcionamiento offline
1. Abrir la app con conexión: se descargan los recordatorios del servidor.
2. Activar **modo avión** en el emulador: aparece "Estás trabajando sin conexión".
3. Crear, editar o eliminar recordatorios: se aplican al instante y quedan "Pendiente de sincronizar".
4. Cerrar la app por completo y volver a abrirla: los datos y las preferencias siguen ahí.
5. Desactivar el modo avión: WorkManager sincroniza solo y las etiquetas de pendiente desaparecen.

En **Logcat**, filtrar por `Repositorio`, `SincronizacionWorker`, `Preferencias` o `ServidorSimulado`.

## Autor

Agustín Ignacio Herner
