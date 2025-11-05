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

        val database = AppDatabase.getDatabase(this)
        repository = TripRepository(
            database.tripDao(),
            database.activityDao(),
            database.journeyLocationDao()
        )

        createNotificationChannel()

        // Callback: ogni nuova posizione ricevuta
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (currentTripId != -1) {
                        Log.d("LocationService", "Nuova posizione: ${location.latitude}, ${location.longitude}")

                        val journeyLocation = JourneyLocation(
                            tripId = currentTripId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = System.currentTimeMillis()
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            repository.insertJourneyLocation(journeyLocation)
                            Log.d("LocationService", "Posizione salvata nel DB")
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Travel Companion")
            .setContentText("Preparazione tracciamento...")
            .setSmallIcon(R.drawable.ic_location)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        when (intent?.action) {
            ACTION_START_TRACKING -> {
                currentTripId = intent.getIntExtra(EXTRA_TRIP_ID, -1)
                if (currentTripId == -1) {
                    Log.e("LocationService", "Trip ID non valido. Arresto del servizio.")
                    stopSelf()
                } else {
                    Log.d("LocationService", "Avvio tracciamento per trip ID: $currentTripId")
                    startLocationUpdates()
                }
            }
            ACTION_STOP_TRACKING -> {
                Log.d("LocationService", "Stop tracciamento richiesto")
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
            .setSmallIcon(R.drawable.ic_location)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000)
            .setMinUpdateIntervalMillis(5_000)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "Permessi mancanti. Stop del servizio.")
            stopSelf()
            return
        }

        // ✅ PRIMA POSIZIONE IMMEDIATA
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && currentTripId != -1) {
                val journeyLocation = JourneyLocation(
                    tripId = currentTripId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis()
                )

                CoroutineScope(Dispatchers.IO).launch {
                    repository.insertJourneyLocation(journeyLocation)
                    Log.d("LocationService", "📍 Prima posizione salvata: ${location.latitude}, ${location.longitude}")
                }
            } else {
                Log.w("LocationService", "Prima posizione non disponibile — attendo fix GPS.")
            }
        }.addOnFailureListener {
            Log.e("LocationService", "Errore nel recupero della prima posizione: ${it.message}")
        }

        // Aggiornamenti continui
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        Log.d("LocationService", "Tracciamento interrotto e foreground fermato.")
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}
