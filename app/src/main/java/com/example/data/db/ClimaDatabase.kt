package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteLocation::class], version = 1, exportSchema = false)
abstract class ClimaDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: ClimaDatabase? = null

        fun getDatabase(context: Context): ClimaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClimaDatabase::class.java,
                    "clima_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
