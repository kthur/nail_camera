package com.example.nailnutri.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NailAnalysisResultEntity::class, SessionReportEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NailDatabase : RoomDatabase() {

    abstract fun nailResultDao(): NailResultDao

    companion object {
        @Volatile
        private var INSTANCE: NailDatabase? = null

        fun getDatabase(context: Context): NailDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NailDatabase::class.java,
                    "nail_nutri_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
