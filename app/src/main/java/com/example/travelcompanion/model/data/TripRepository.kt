package com.example.travelcompanion.model.data

import com.example.travelcompanion.model.Trip
import com.example.travelcompanion.model.TripActivity
import com.example.travelcompanion.model.data.dao.ActivityDao
import com.example.travelcompanion.model.data.dao.JourneyLocationDao
import com.example.travelcompanion.model.data.dao.TripDao
import com.example.travelcompanion.model.data.entities.JourneyLocation
import kotlinx.coroutines.flow.Flow // Usa Flow invece di LiveData qui

class TripRepository(
    private val tripDao: TripDao,
    private val activityDao: ActivityDao,
    private val journeyLocationDao: JourneyLocationDao
) {

    // Cambiato LiveData in Flow
    val allTrips: Flow<List<Trip>> = tripDao.getAllTripsFlow()

    // Cambiato LiveData in Flow
    fun getTripById(tripId: Int): Flow<Trip?> { // Potrebbe essere nullo
        return tripDao.getTripByIdFlow(tripId)
    }

    // Assicurati che il DAO abbia un metodo insertTrip
    suspend fun insertTrip(trip: Trip) {
        tripDao.insertTrip(trip)
    }

    // Assicurati che il DAO abbia un metodo deleteTrip
    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteTrip(trip)
    }

    // Assicurati che il DAO abbia un metodo updateTrip (se necessario)
    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip)
    }


    // Funzioni per TripActivity - Usa Flow
    fun getActivitiesForTrip(tripId: Int): Flow<List<TripActivity>> {
        return activityDao.getActivitiesForTripFlow(tripId)
    }

    // Assicurati che il DAO abbia insertActivity
    suspend fun insertActivity(activity: TripActivity) {
        activityDao.insertActivity(activity)
    }

    // Assicurati che il DAO abbia deleteActivity (se necessario)
    suspend fun deleteActivity(activity: TripActivity) {
        activityDao.deleteActivity(activity)
    }


    // Funzione per salvare le coordinate
    suspend fun insertJourneyLocation(location: JourneyLocation) {
        journeyLocationDao.insertLocation(location) // Assicurati che il DAO abbia insertLocation
    }

    // Funzione per prendere le coordinate (per StatsFragment) - Usa Flow
    fun getLocationsForPeriod(startTime: Long, endTime: Long): Flow<List<JourneyLocation>> {
        return journeyLocationDao.getLocationsForPeriodFlow(startTime, endTime) // Assicurati che il DAO abbia questo metodo Flow
    }
}