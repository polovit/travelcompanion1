package com.example.travelcompanion.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.travelcompanion.LocationTrackingService
import com.example.travelcompanion.R

class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var currentTripId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startButton = view.findViewById(R.id.startJourneyButton)
        stopButton = view.findViewById(R.id.stopJourneyButton)

        currentTripId = arguments?.getInt("tripId", -1) ?: -1

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
}
