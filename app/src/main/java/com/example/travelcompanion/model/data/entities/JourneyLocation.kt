package com.example.travelcompanion.model.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "journey_location")
data class JourneyLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tripId: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val cityName: String? = null
)
