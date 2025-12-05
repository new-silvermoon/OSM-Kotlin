package org.silvermoon.osm_kotlin.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.silvermoon.osm_kotlin.model.entities.TileDao
import org.silvermoon.osm_kotlin.model.entities.TileEntity

@Database(entities = [TileEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tileDao(): TileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "osm_db.sqlite"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
