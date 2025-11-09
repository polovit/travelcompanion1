package com.example.travelcompanion.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destination: String,
    val startDate: String?=null ,
    val endDate: String?=null ,
    val type: String? = null,
    val isOngoing: Boolean = false //  indica se il viaggio è “attivo”

)
