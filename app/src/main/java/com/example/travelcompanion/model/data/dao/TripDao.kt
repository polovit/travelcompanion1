package com.example.travelcompanion.model.data.dao

import androidx.room.*
import com.example.travelcompanion.model.Trip
import kotlinx.coroutines.flow.Flow // Importa Flow

@Dao
interface TripDao {
    // Usa Flow qui
    @Query("SELECT * FROM trips ORDER BY id DESC")
    fun getAllTripsFlow(): Flow<List<Trip>>

    // Usa Flow qui
    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripByIdFlow(tripId: Int): Flow<Trip?> // Può essere nullo

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip) // Nome corretto

    @Update
    suspend fun updateTrip(trip: Trip) // Aggiunto/Verificato
    // ...

    // Funzione per il Worker: prende l'ultimo viaggio (non serve Flow)
    @Query("SELECT * FROM trips ORDER BY id DESC LIMIT 1")
    suspend fun getLatestTrip(): Trip?



    @Delete
    suspend fun deleteTrip(trip: Trip) // Nome corretto
}