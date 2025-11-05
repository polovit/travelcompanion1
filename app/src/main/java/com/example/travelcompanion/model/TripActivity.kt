package com.example.travelcompanion.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
        tableName = "trip_activities",
        foreignKeys = [ForeignKey(
                entity = Trip::class,
                parentColumns = ["id"],
                childColumns = ["tripId"],
                onDelete = ForeignKey.CASCADE
        )]
)
data class TripActivity(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val tripId: Int,
        val timestamp: Long,
        val latitude: Double?,
        val longitude: Double?,
        val note: String?,
        val photoPath: String? // path dell’immagine salvata localmente
)
