package com.example.travelcompanion.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.travelcompanion.model.Trip
import com.example.travelcompanion.model.TripActivity
import com.example.travelcompanion.model.data.AppDatabase
import com.example.travelcompanion.model.data.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TripsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository


    //  Definiamo uno "stato" per il filtro. null = "Tutti"
    private val filterState = MutableStateFlow<String?>(null)

    //  allTrips ora combina il flusso dal repository CON il flusso del filtro
    val allTrips: LiveData<List<Trip>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TripRepository(database.tripDao(), database.activityDao(), database.journeyLocationDao())

        // Combina i due flussi:
        allTrips = repository.allTrips.combine(filterState) { trips, filter ->
            if (filter == null) {
                // Se il filtro è nullo, restituisci tutti i viaggi
                trips
            } else {
                // Altrimenti, filtra la lista in base al tipo
                trips.filter { it.type == filter }
            }
        }.asLiveData() // Converti il risultato in LiveData
    }


    fun setFilter(type: String?) {
        filterState.value = type
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
    fun getTripById(tripId: Int): LiveData<Trip?> {
        return repository.getTripById(tripId).asLiveData()
    }


    // Espone il LiveData per le posizioni di un viaggio
    fun getLocationsForTrip(tripId: Int): LiveData<List<com.example.travelcompanion.model.data.entities.JourneyLocation>> {
        return repository.getLocationsForTrip(tripId).asLiveData()
    }
}