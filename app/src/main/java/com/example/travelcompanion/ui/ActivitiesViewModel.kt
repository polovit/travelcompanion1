package com.example.travelcompanion.ui

import android.app.Application
import androidx.lifecycle.* // Importa tutto
import com.example.travelcompanion.model.TripActivity
import com.example.travelcompanion.model.data.AppDatabase
import com.example.travelcompanion.model.data.TripRepository
import kotlinx.coroutines.flow.Flow // Importa Flow
import kotlinx.coroutines.launch

class ActivitiesViewModel(application: Application) : AndroidViewModel(application) {

        private val repository: TripRepository

        init {
                val db = AppDatabase.getDatabase(application)
                // Passa tutti i DAO necessari
                repository = TripRepository(db.tripDao(), db.activityDao(), db.journeyLocationDao())
        }

        // Restituisce LiveData per l'UI
        fun getActivitiesForTrip(tripId: Int): LiveData<List<TripActivity>> {
                return repository.getActivitiesForTrip(tripId).asLiveData()
        }

        fun insertActivity(activity: TripActivity) = viewModelScope.launch {
                repository.insertActivity(activity)
        }

        fun deleteActivity(activity: TripActivity) = viewModelScope.launch {
                repository.deleteActivity(activity)
        }
}