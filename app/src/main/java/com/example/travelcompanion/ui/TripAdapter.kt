package com.example.travelcompanion.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.databinding.ItemTripBinding
import com.example.travelcompanion.model.Trip

// Costruttore corretto: accetta solo le funzioni di callback
class TripAdapter(
    private val onTripClick: (Trip) -> Unit,
    private val onTripDelete: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    // Lista interna per memorizzare i dati
    private var trips: List<Trip> = emptyList()

    // ViewHolder che usa View Binding
    inner class TripViewHolder(private val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root) {
        // Metodo per collegare i dati di un Trip alla view
        fun bind(trip: Trip) {
            // Usa gli ID corretti dal tuo file item_trip.xml
            binding.textDestination.text = trip.destination
            binding.textType.text = trip.type ?: "N/A" // Gestisci il caso in cui type sia null

            // Imposta il click listener per l'intera riga
            binding.root.setOnClickListener {

                onTripClick(trip)
            }


            binding.deleteButton.setOnClickListener {
                // Chiama la funzione lambda passata dal Fragment
                onTripDelete(trip)
            }
        }
    }

    // Crea un nuovo ViewHolder quando RecyclerView ne ha bisogno
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        // Infla il layout dell'item usando View Binding
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }

    // Collega i dati alla vista per una specifica posizione
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    // Restituisce il numero totale di elementi nella lista
    override fun getItemCount(): Int = trips.size

    // Metodo pubblico per aggiornare la lista di viaggi mostrata dall'adapter
    fun updateList(newTrips: List<Trip>) {
        trips = newTrips
        notifyDataSetChanged()
    }
}