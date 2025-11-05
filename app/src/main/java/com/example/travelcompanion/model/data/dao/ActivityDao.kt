package com.example.travelcompanion.model.data.dao

import androidx.room.*
import com.example.travelcompanion.model.TripActivity
import kotlinx.coroutines.flow.Flow // Importa Flow

@Dao
interface ActivityDao {
    // Usa Flow qui
    @Query("SELECT * FROM trip_activities WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getActivitiesForTripFlow(tripId: Int): Flow<List<TripActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: TripActivity) // Nome corretto

    @Delete
    suspend fun deleteActivity(activity: TripActivity) // Aggiunto/Verificato
}