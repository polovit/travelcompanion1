package com.example.travelcompanion

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.travelcompanion.model.data.AppDatabase
import com.example.travelcompanion.model.data.TripRepository
import com.example.travelcompanion.model.data.entities.JourneyLocation
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: TripRepository
    private var currentTripId: Int = -1

    companion object {
        const val ACTION_START_TRACKING = "com.example.travelcompanion.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.travelcompanion.ACTION_STOP_TRACKING"
        const val EXTRA_TRIP_ID = "extra_trip_id"
        private const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inizializza il repository
        val database = AppDatabase.getDatabase(this)
        repository = TripRepository(database.tripDao(), database.activityDao(), database.journeyLocationDao())

        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (currentTripId != -1) {
                        Log.d("LocationService", "Location received: ${location.latitude}, ${location.longitude}")

                        val journeyLocation = JourneyLocation(
                            tripId = currentTripId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = System.currentTimeMillis()
                        )

                        // Salva nel database in background
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.insertJourneyLocation(journeyLocation)
                        }
                    }
                }
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1️⃣ Crea e mostra la notifica SUBITO
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Travel Companion")
            .setContentText("Inizializzazione del tracciamento...")
            .setSmallIcon(R.drawable.ic_location)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        // 2️⃣ Poi analizza l’intent
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                currentTripId = intent.getIntExtra(EXTRA_TRIP_ID, -1)
                if (currentTripId == -1) {
                    Log.e("LocationService", "Invalid Trip ID, stopping service.")
                    stopSelf()
                } else {
                    Log.d("LocationService", "Starting tracking for trip ID: $currentTripId")
                    startLocationUpdates()
                }
            }
            ACTION_STOP_TRACKING -> {
                Log.d("LocationService", "Stopping tracking")
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Travel Companion")
            .setContentText("Registrazione del viaggio in corso...")
            .setSmallIcon(R.drawable.ic_location) // Assicurati di avere 'ic_location' in res/drawable
            .build()

        startForeground(NOTIFICATION_ID, notification)

        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 secondi
            fastestInterval = 5000 // 5 secondi
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // Controlla di nuovo i permessi (anche se dovrebbero essere già stati concessi)
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "Location permission not granted. Stopping service.")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}