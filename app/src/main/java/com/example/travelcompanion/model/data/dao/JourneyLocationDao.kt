package com.example.travelcompanion.model.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.travelcompanion.model.data.entities.JourneyLocation
import kotlinx.coroutines.flow.Flow // Importa Flow

@Dao
interface JourneyLocationDao {



    @Insert
    suspend fun insertLocation(location: JourneyLocation) // Nome corretto

    // Metodo per StatsFragment - Usa Flow
    @Query("SELECT * FROM journey_location WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getLocationsForPeriodFlow(start: Long, end: Long): Flow<List<JourneyLocation>>

    @Query("SELECT * FROM journey_location WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getLocationsForTrip(tripId: Int): List<JourneyLocation>
    // Metodi aggiuntivi che avevi, assicurati siano corretti
    @Query("SELECT COUNT(DISTINCT tripId) FROM journey_location")
    suspend fun getDistinctCitiesCount(): Int

    @Query("SELECT * FROM journey_location WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getAllBetween(start: Long, end: Long): List<JourneyLocation> // Questo è suspend, ok

    // Aggiungi questo se non c'è già, per il Repository
    @Query("SELECT * FROM journey_location WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getLocationsForPeriod(start: Long, end: Long): Flow<List<JourneyLocation>> // Questo è Flow
}