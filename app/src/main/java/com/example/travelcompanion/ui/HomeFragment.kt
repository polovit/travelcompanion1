package com.example.travelcompanion.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val addTripButton = view.findViewById<Button>(R.id.addTripButton)
        val viewStatsButton = view.findViewById<Button>(R.id.viewStatsButton)

// start tracking (foreground)



        // Vai a TripsFragment
        addTripButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, TripsFragment())
                .addToBackStack(null)
                .commit()
        }

        // Vai a StatsFragment
        viewStatsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, StatsFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
