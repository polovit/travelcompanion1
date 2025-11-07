package com.example.travelcompanion.ui

import android.app.Application
import androidx.lifecycle.* // Importa tutto da lifecycle
import com.example.travelcompanion.model.Trip
import com.example.travelcompanion.model.TripActivity
import com.example.travelcompanion.model.data.AppDatabase
import com.example.travelcompanion.model.data.TripRepository
import kotlinx.coroutines.flow.Flow // Importa Flow
import kotlinx.coroutines.launch

class TripsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository
    // Usa asLiveData() per convertire Flow in LiveData per l'UI
    val allTrips: LiveData<List<Trip>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TripRepository(database.tripDao(), database.activityDao(), database.journeyLocationDao())
        // Converti il Flow in LiveData
        allTrips = repository.allTrips.asLiveData()
    }

    fun insertTrip(trip: Trip) = viewModelScope.launch {
        repository.insertTrip(trip)
    }

    fun deleteTrip(trip: Trip) = viewModelScope.launch {
        repository.deleteTrip(trip)
    }

    fun updateTrip(trip: Trip) = viewModelScope.launch {
        repository.updateTrip(trip)
    }


    // Restituisce LiveData per l'UI
    fun getActivitiesForTrip(tripId: Int): LiveData<List<TripActivity>> {
        return repository.getActivitiesForTrip(tripId).asLiveData()
    }

    fun addActivity(activity: TripActivity) = viewModelScope.launch {
        repository.insertActivity(activity)
    }
    // Espone il LiveData per le posizioni di un viaggio
    fun getLocationsForTrip(tripId: Int): LiveData<List<com.example.travelcompanion.model.data.entities.JourneyLocation>> {
        return repository.getLocationsForTrip(tripId).asLiveData()
    }
}