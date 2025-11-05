package com.example.travelcompanion.model.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.travelcompanion.model.Trip
import com.example.travelcompanion.model.TripActivity
import com.example.travelcompanion.model.data.dao.TripDao
import com.example.travelcompanion.model.data.dao.ActivityDao
import com.example.travelcompanion.model.data.dao.JourneyLocationDao
import com.example.travelcompanion.model.data.entities.JourneyLocation

@Database(
    entities = [Trip::class, TripActivity::class, JourneyLocation::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journeyLocationDao(): JourneyLocationDao
    abstract fun tripDao(): TripDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travel_companion_db"
                )
                    .fallbackToDestructiveMigration() // forza ricreazione se cambia lo schema
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
