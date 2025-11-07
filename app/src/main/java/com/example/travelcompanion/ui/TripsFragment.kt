package com.example.travelcompanion.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.DialogAddTripBinding
import com.example.travelcompanion.databinding.FragmentTripsBinding
import com.example.travelcompanion.model.Trip
import java.text.DateFormat
import java.util.Date
import android.app.DatePickerDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.MenuInflater



class TripsFragment : Fragment() {

    // USARE L'ALTRO VIEWMODEL SE NECESSARIO, MA TRIPSVIEWMODEL HA GIÀ TUTTO
    private val tripsViewModel: TripsViewModel by viewModels()
    private lateinit var adapter: TripAdapter // Definizione dell'adapter

    // --- VIEW BINDING per FragmentTrips ---
    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!
    // --- FINE VIEW BINDING ---
    private var trips: List<Trip> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        val view = binding.root

        // --- Configurazione Adapter ---
        adapter = TripAdapter(
            onTripClick = { trip ->
                // QUI è sicuro usare findNavController()
                val action = TripsFragmentDirections.actionNavTripsToTripDetailsFragment(trip.id)
                findNavController().navigate(action)
            },
            onTripDelete = { trip ->
                showDeleteConfirmationDialog(trip)
            }
        )
        // --- Fine Configurazione Adapter ---

        binding.recyclerViewTrips.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTrips.adapter = adapter

        tripsViewModel.allTrips.observe(viewLifecycleOwner) { trips ->
            trips?.let {
                adapter.updateList(it)
                Log.d("TripsFragment", "Trips updated: ${it.size}")
            }
        }

        binding.addTripFab.setOnClickListener {
            showAddTripDialog()
        }

        return view
    }



    // --- VIEW BINDING ---
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Pulisci il riferimento al binding
    }
    // --- FINE VIEW BINDING ---

    private fun showAddTripDialog() {
        val dialogBinding = DialogAddTripBinding.inflate(LayoutInflater.from(requireContext()))

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Gestione del cambio tipo di viaggio
        dialogBinding.tripTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                dialogBinding.radioLocal.id -> {
                    dialogBinding.startDateLayout.visibility = View.VISIBLE
                    dialogBinding.endDateLayout.visibility = View.GONE
                }
                dialogBinding.radioDay.id, dialogBinding.radioMultiDay.id -> {
                    dialogBinding.startDateLayout.visibility = View.VISIBLE
                    dialogBinding.endDateLayout.visibility = View.VISIBLE
                }
            }
        }

        // Selettori data
        val datePickerListener = { editText: TextInputEditText ->
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = "$dayOfMonth/${month + 1}/$year"
                    editText.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.startDateEditText.setOnClickListener { datePickerListener(dialogBinding.startDateEditText) }
        dialogBinding.endDateEditText.setOnClickListener { datePickerListener(dialogBinding.endDateEditText) }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setTitle("Add New Trip")
            .setPositiveButton("Add") { dialog, _ ->
                val destination = dialogBinding.destinationEditText.text.toString()
                val selectedId = dialogBinding.tripTypeRadioGroup.checkedRadioButtonId
                val type = when (selectedId) {
                    dialogBinding.radioLocal.id -> "Local trip"
                    dialogBinding.radioDay.id -> "Day trip"
                    dialogBinding.radioMultiDay.id -> "Multi-day trip"
                    else -> null
                }

                val startDate = dialogBinding.startDateEditText.text.toString()
                val endDate = dialogBinding.endDateEditText.text.toString()

                // Validazioni logiche
                if (destination.isBlank() || type == null) {
                    Toast.makeText(context, "Inserisci destinazione e tipo di viaggio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                when (type) {
                    "Local trip" -> {
                        if (startDate.isBlank()) {
                            Toast.makeText(context, "Inserisci una data per il viaggio locale", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                    "Day trip", "Multi-day trip" -> {
                        if (startDate.isBlank() || endDate.isBlank()) {
                            Toast.makeText(context, "Inserisci data di inizio e fine", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                }

                val newTrip = Trip(
                    destination = destination,
                    type = type,
                    startDate = startDate,
                    endDate = if (endDate.isBlank()) null else endDate
                )
                tripsViewModel.insertTrip(newTrip)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }


    private fun showDeleteConfirmationDialog(trip: Trip) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete the trip to ${trip.destination}?")
            .setPositiveButton("Delete") { _, _ ->
                tripsViewModel.deleteTrip(trip)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_sort, menu)
        super.onCreateOptionsMenu(menu, inflater)
        // Mostra le icone anche nel menu overflow
        if (menu is androidx.appcompat.view.menu.MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort_by_name -> {
                sortTripsByName()
                true
            }
            R.id.sort_by_date -> {
                sortTripsByDate()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun sortTripsByName() {
        // ordina alfabeticamente per destinazione
        val sortedList = trips.sortedBy { it.destination.lowercase() }
        adapter.updateList(sortedList)
        Toast.makeText(requireContext(), "Ordinati per destinazione", Toast.LENGTH_SHORT).show()
    }

    private fun sortTripsByDate() {
        // ordina per data di inizio (più recenti per primi)
        val sortedList = trips.sortedByDescending { it.startDate }
        adapter.updateList(sortedList)
        Toast.makeText(requireContext(), "Ordinati per data", Toast.LENGTH_SHORT).show()
    }


}