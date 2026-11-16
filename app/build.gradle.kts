import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Rutas tipadas de Navigation Compose y DTO del servicio remoto: clases @Serializable
    alias(libs.plugins.kotlin.serialization)
    // Actividad 3: procesador de anotaciones de Room
    alias(libs.plugins.ksp)
}

// ---------------------------------------------------------------------------
// Configuración del servicio remoto (consigna 4).
// Se lee de remote.properties (NO se sube a Git). Si el archivo no existe se
// usan valores por defecto que activan el servidor simulado, así el proyecto
// compila y funciona sin configurar nada. Ver remote.properties.example.
// ---------------------------------------------------------------------------
val propiedadesRemotas = Properties().apply {
    val archivo = rootProject.file("remote.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiBaseUrl: String = propiedadesRemotas.getProperty("API_BASE_URL", "https://api.recordatorios.simulado/")
val apiSimulada: String = propiedadesRemotas.getProperty("API_SIMULADA", "true")

android {
    namespace = "com.example.recordatorios"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.recordatorios"
        minSdk = 24
        targetSdk = 37
        // Tercera versión instalable
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Valores accesibles desde el código como BuildConfig.API_BASE_URL / BuildConfig.API_SIMULADA
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "API_SIMULADA", apiSimulada)
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true   // necesario para buildConfigField
    }
}

// Room exporta el esquema de la base (exportSchema = true) a esta carpeta.
// Sirve para versionar el esquema y para crear y probar migraciones futuras.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Actividad 2: ViewModel en Compose, collectAsStateWithLifecycle, navegación tipada e íconos
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.material.icons.core)
    // Actividad 3: preferencias, base local, servicio remoto y sincronización
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)   // detectar cuando la app vuelve a primer plano
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
