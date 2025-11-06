package com.example.travelcompanion.ui
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.LocationTrackingService
import com.example.travelcompanion.R

class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var addNoteButton: Button
    private lateinit var addPhotoButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    private val activities = mutableListOf<String>()
    private lateinit var adapter: ActivityAdapter

    private var currentTripId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Binding elementi UI ---
        startButton = view.findViewById(R.id.startJourneyButton)
        stopButton = view.findViewById(R.id.stopJourneyButton)
        addNoteButton = view.findViewById(R.id.addNoteButton)
        addPhotoButton = view.findViewById(R.id.addPhotoButton)
        recyclerView = view.findViewById(R.id.activitiesRecyclerView)

        currentTripId = arguments?.getInt("tripId", -1) ?: -1

        // --- RecyclerView setup ---
        adapter = ActivityAdapter(activities)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // --- Listener per tracking ---
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

        // --- Listener per note ---
        addNoteButton.setOnClickListener {
            showNoteDialog()
        }

        // --- Listener per foto ---
        addPhotoButton.setOnClickListener {
            Toast.makeText(requireContext(), "Funzione foto in sviluppo", Toast.LENGTH_SHORT).show()
        }
        // --- Inizializzazione MAPPA ---
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            googleMap = map
            googleMap?.uiSettings?.isZoomControlsEnabled = true

            // Posizione iniziale (puoi cambiarla)
            val bologna = LatLng(44.4949, 11.3426)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(bologna, 13f))
        }

    }

    // ---- Dialog per aggiungere nota ----
    private fun showNoteDialog() {
        val input = EditText(requireContext())
        input.hint = "Scrivi una nota..."
        AlertDialog.Builder(requireContext())
            .setTitle("Aggiungi nota")
            .setView(input)
            .setPositiveButton("Salva") { _, _ ->
                val note = input.text.toString().trim()
                if (note.isNotBlank()) {
                    activities.add("📝 $note")
                    adapter.notifyItemInserted(activities.size - 1)
                } else {
                    Toast.makeText(requireContext(), "Nota vuota", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
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

    // ---- Avvio tracciamento ----
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
    }

    // ---- Arresto tracciamento ----
    private fun stopTracking() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        requireContext().startService(intent)

        Toast.makeText(requireContext(), "Tracciamento terminato", Toast.LENGTH_SHORT).show()
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
    }
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
