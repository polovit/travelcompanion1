package com.example.travelcompanion.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.R
import com.example.travelcompanion.model.data.AppDatabase
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripDetailsFragment : Fragment(R.layout.fragment_trip_details), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var map: GoogleMap? = null
    private var currentTripId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupera il tripId passato
        currentTripId = arguments?.getInt("tripId", -1) ?: -1

        mapView = view.findViewById(R.id.tripMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true
        loadTripPath()
    }

    private fun loadTripPath() {
        if (currentTripId == -1) {
            Toast.makeText(requireContext(), "ID viaggio non valido", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val locations = db.journeyLocationDao().getLocationsForTrip(currentTripId)

            withContext(Dispatchers.Main) {
                if (locations.isEmpty()) {
                    Toast.makeText(requireContext(), "Nessun percorso registrato", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val path = locations.map { LatLng(it.latitude, it.longitude) }
                val polyline = PolylineOptions()
                    .addAll(path)
                    .width(8f)
                    .color(resources.getColor(R.color.teal_700, null))

                map?.apply {
                    addPolyline(polyline)
                    moveCamera(CameraUpdateFactory.newLatLngZoom(path.first(), 15f))
                }
            }
        }
    }

    // Ciclo di vita della mappa
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
