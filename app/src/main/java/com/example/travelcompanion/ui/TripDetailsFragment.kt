package com.example.travelcompanion.ui
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import android.annotation.SuppressLint
import androidx.fragment.app.viewModels
import com.example.travelcompanion.model.TripActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.Polyline
import com.example.travelcompanion.model.data.TripRepository
import com.google.android.gms.maps.model.MarkerOptions
import com.example.travelcompanion.ui.adapters.TripActivityAdapter
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.LocationTrackingService
import com.example.travelcompanion.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker

// Nota: non abbiamo più bisogno di lifecycleScope, Dispatchers, o AppDatabase qui

class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    // --- Elementi UI ---
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var addNoteButton: Button
    private lateinit var addPhotoButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var mapView: MapView

    // --- Logica Mappa ---
    private var googleMap: GoogleMap? = null
    private var currentPolyline: Polyline? = null
    private val currentMarkers: MutableList<Marker> = mutableListOf()

    // --- Adapter e ViewModel ---
    private lateinit var activityAdapter: TripActivityAdapter
    private val tripsViewModel: TripsViewModel by viewModels()

    // --- Logica Posizione ---
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentTripId: Int = -1

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Binding elementi UI ---
        startButton = view.findViewById(R.id.startJourneyButton)
        stopButton = view.findViewById(R.id.stopJourneyButton)
        addNoteButton = view.findViewById(R.id.addNoteButton)
        addPhotoButton = view.findViewById(R.id.addPhotoButton)
        recyclerView = view.findViewById(R.id.activitiesRecyclerView)
        mapView = view.findViewById(R.id.mapView)

        currentTripId = arguments?.getInt("tripId", -1) ?: -1
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // --- Setup RecyclerView e Adapter ---
        setupRecyclerView()

        // --- Setup Osservatori Reattivi ---
        setupObservers()

        // --- Setup Listener Pulsanti ---
        setupButtonListeners()

        // --- Inizializzazione MAPPA ---
        setupMap(savedInstanceState)

        centerMapOnCurrentLocation()
    }

    private fun setupRecyclerView() {
        activityAdapter = TripActivityAdapter { activity ->
            // La tua idea: "Clicca nota per centrare mappa"
            if (activity.latitude != null && activity.longitude != null) {
                val position = LatLng(activity.latitude, activity.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))
            } else {
                Toast.makeText(requireContext(), "Nessuna posizione per questa nota", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = activityAdapter
    }

    private fun setupObservers() {
        if (currentTripId != -1) {
            // 1. Osserva le attività (NOTE/PIN)
            tripsViewModel.getActivitiesForTrip(currentTripId).observe(viewLifecycleOwner) { activities ->
                activityAdapter.setActivities(activities)
                drawActivityMarkers(activities) // Disegna solo i pin
            }

            // 2. Osserva le POSIZIONI (PERCORSO)
            tripsViewModel.getLocationsForTrip(currentTripId).observe(viewLifecycleOwner) { points ->
                drawTripPolyline(points) // Disegna solo la linea
            }
        }
    }

    private fun setupButtonListeners() {
        startButton.setOnClickListener {
            if (!checkLocationPermissions()) {
                requestLocationPermissions()
            } else {
                startTracking()
            }
        }

        stopButton.setOnClickListener {
            stopTracking()
        }

        addNoteButton.setOnClickListener {
            showNoteDialog()
        }

        addPhotoButton.setOnClickListener {
            Toast.makeText(requireContext(), "Funzione foto in sviluppo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            googleMap = map
            googleMap?.uiSettings?.isZoomControlsEnabled = true

            // Non disegniamo nulla qui, gli osservatori lo faranno
            // appena i dati arrivano dal database.

            if (currentTripId == -1) {
                // Mostra solo Bologna come fallback
                val bologna = LatLng(44.4949, 11.3426)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(bologna, 13f))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNoteDialog() {
        if (!checkLocationPermissions()) {
            Toast.makeText(requireContext(), "Permessi di posizione necessari", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val input = EditText(requireContext())
            input.hint = "Scrivi una nota..."

            AlertDialog.Builder(requireContext())
                .setTitle("Aggiungi nota")
                .setView(input)
                .setPositiveButton("Salva") { _, _ ->
                    val note = input.text.toString().trim()
                    if (note.isNotBlank() && currentTripId != -1) {
                        val newActivity = TripActivity(
                            tripId = currentTripId,
                            timestamp = System.currentTimeMillis(),
                            latitude = location?.latitude, // Può essere null
                            longitude = location?.longitude, // Può essere null
                            note = note,
                            photoPath = null
                        )
                        tripsViewModel.addActivity(newActivity)
                        Toast.makeText(requireContext(), "Nota salvata", Toast.LENGTH_SHORT).show()
                    } else if (note.isBlank()) {
                        Toast.makeText(requireContext(), "Nota vuota", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Errore salvataggio nota", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()

        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Impossibile ottenere la posizione", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Gestione permessi ----
    private fun checkLocationPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED && coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1001
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTracking()
        } else {
            Toast.makeText(requireContext(), "Permesso posizione negato", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Avvio tracciamento (PULITO) ----
    private fun startTracking() {
        if (currentTripId == -1) {
            Toast.makeText(requireContext(), "Errore: ID viaggio non valido", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
            putExtra(LocationTrackingService.EXTRA_TRIP_ID, currentTripId)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
        Toast.makeText(requireContext(), "Tracciamento avviato", Toast.LENGTH_SHORT).show()

        startButton.visibility = View.GONE
        stopButton.visibility = View.VISIBLE
        centerMapOnCurrentLocation()
        // Non disegniamo più nulla da qui
    }

    // ---- Arresto tracciamento (PULITO) ----
    private fun stopTracking() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        requireContext().startService(intent)

        Toast.makeText(requireContext(), "Tracciamento terminato", Toast.LENGTH_SHORT).show()
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        centerMapOnCurrentLocation()
        // Non disegniamo più nulla da qui
    }

    // ---- Funzioni di Disegno Specializzate ----

    /**
     * Disegna SOLO i pin delle attività (note/foto).
     * Pulisce solo i pin vecchi, non tocca la linea.
     */
    private fun drawActivityMarkers(activities: List<TripActivity>) {
        googleMap?.let { map ->
            // Pulisce i vecchi marker
            currentMarkers.forEach { it.remove() }
            currentMarkers.clear()

            // Aggiungi un marker per ogni attività
            activities.forEach { activity ->
                if (activity.latitude != null && activity.longitude != null) {
                    val latLng = LatLng(activity.latitude, activity.longitude)
                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(activity.note ?: "Attività")
                            .alpha(0.8f)
                    )
                    if (marker != null) {
                        currentMarkers.add(marker) // Tieni traccia del marker
                    }
                }
            }
        }
    }

    /**
     * Disegna SOLO la linea del percorso.
     * Pulisce solo la linea vecchia, non tocca i pin.
     */
    private fun drawTripPolyline(points: List<com.example.travelcompanion.model.data.entities.JourneyLocation>) {
        googleMap?.let { map ->
            // Pulisce la vecchia polyline
            currentPolyline?.remove()

            if (points.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .color(Color.BLUE)
                    .width(6f)
                    // Converte la lista di JourneyLocation in lista di LatLng
                    .addAll(points.map { LatLng(it.latitude, it.longitude) })

                currentPolyline = map.addPolyline(polylineOptions) // Salva la nuova polyline
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun centerMapOnCurrentLocation() {
        // Controlla solo se abbiamo i permessi, altrimenti non fare nulla
        if (checkLocationPermissions()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    Toast.makeText(requireContext(), "Posizione non disponibile, attendere il fix GPS", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Impossibile ottenere la posizione attuale", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Gestione ciclo di vita MapView ---
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}