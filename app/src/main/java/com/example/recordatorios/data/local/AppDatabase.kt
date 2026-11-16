package com.example.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos Room de la app (Módulo 3 · U1 · Tema 2, "Creación de la base de datos").
 *
 * Reúne las entidades, declara la versión del esquema y entrega los DAO.
 *
 * - version = 1: es la primera versión con base de datos. Si una
 *   actualización futura cambia la tabla (por ejemplo, agrega una columna),
 *   hay que subir el número y declarar una migración (AutoMigration o
 *   Migration manual). NO se usa fallbackToDestructiveMigration: borraría los
 *   recordatorios creados sin conexión que todavía no se sincronizaron.
 * - exportSchema = true: guarda el esquema en app/schemas/ (configurado en
 *   build.gradle.kts) para versionarlo y probar migraciones.
 */
@Database(
    entities = [RecordatorioEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        private const val NOMBRE_ARCHIVO = "recordatorios.db"

        /**
         * Construye la base. Se llama UNA sola vez desde ContenedorDependencias,
         * que conserva la instancia y la comparte. Crear una instancia por
         * pantalla gastaría recursos y complicaría coordinar las operaciones.
         * Se usa applicationContext para no retener una Activity.
         */
        fun crear(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                NOMBRE_ARCHIVO
            ).build()
    }
}
