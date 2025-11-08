package com.example.travelcompanion.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.R
import com.example.travelcompanion.model.data.AppDatabase
import com.example.travelcompanion.model.data.entities.JourneyLocation
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.math.*

class StatsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var totalKmText: TextView
    private lateinit var citiesVisitedText: TextView
    private var map: GoogleMap? = null

    // --- VARIABILE AGGIUNTA ---
    private lateinit var barChart: BarChart
    // --- FINE VARIABILE ---

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        totalKmText = view.findViewById(R.id.totalKmText)
        citiesVisitedText = view.findViewById(R.id.citiesVisitedText)

        // --- RIGA AGGIUNTA ---
        barChart = view.findViewById(R.id.distanceBarChart) // Collega il grafico
        // --- FINE RIGA ---

        val mapFragment = childFragmentManager.findFragmentById(R.id.statsMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadStats()

        // --- RIGA AGGIUNTA ---
        loadBarChartData() // Avvia il caricamento del grafico
        // --- FINE RIGA ---

        return view
    }

    private fun loadStats() {
        val db = AppDatabase.getDatabase(requireContext())
        val dao = db.journeyLocationDao()

        lifecycleScope.launch { // Parte sul Main
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val endOfMonth = now.plusMonths(1).withDayOfMonth(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

            // Passa al thread background (IO) per TUTTO il lavoro pesante
            val (totalKm, cityCount, pathPoints) = withContext(Dispatchers.IO) {
                val locations = dao.getAllBetween(startOfMonth, endOfMonth)

                if (locations.isEmpty()) {
                    // Se non c'è niente, restituisci valori vuoti
                    Triple(0.0, 0, emptyList<LatLng>())
                } else {
                    // Esegui i calcoli pesanti QUI, sul thread background
                    val km = calculateTotalKm(locations)
                    val cities = countCitiesVisited(locations)
                    val points = locations.map { LatLng(it.latitude, it.longitude) }

                    // Restituisci i risultati
                    Triple(km, cities, points)
                }
            }

            // Ora sei di nuovo sul Main thread, ma hai solo risultati pronti
            // Aggiornare la UI è velocissimo
            totalKmText.text = "Totale km: %.2f".format(totalKm)
            citiesVisitedText.text = "Città visitate: $cityCount"

            // Aggiorna la mappa (sul Main thread)
            if (pathPoints.isNotEmpty()) {
                map?.apply {
                    addPolyline(
                        PolylineOptions()
                            .addAll(pathPoints)
                            .width(8f)
                    )
                    moveCamera(CameraUpdateFactory.newLatLngZoom(pathPoints.last(), 6f))
                }
            }
        }
    }

    // Le tue funzioni calculateTotalKm, haversine, e countCitiesVisited
    // non devono essere modificate.
    private fun calculateTotalKm(locations: List<JourneyLocation>): Double {
        var total = 0.0
        for (i in 0 until locations.size - 1) {
            val a = locations[i]
            val b = locations[i + 1]
            total += haversine(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return total
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        return 2 * R * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun countCitiesVisited(locations: List<JourneyLocation>): Int {
        // semplice placeholder: considera una “città” ogni ~50km di distanza
        if (locations.isEmpty()) return 0
        var count = 1
        var last = locations.first()
        for (loc in locations.drop(1)) {
            if (haversine(last.latitude, last.longitude, loc.latitude, loc.longitude) > 50) {
                count++
                last = loc
            }
        }
        return count
    }

    private fun drawPathOnMap(locations: List<JourneyLocation>) {
        map?.let { googleMap ->
            if (locations.isEmpty()) return
            val points = locations.map { LatLng(it.latitude, it.longitude) }
            googleMap.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(8f)
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.last(), 6f))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    // --- BLOCCO INTERO DA AGGIUNGERE ALLA FINE DEL FILE ---

    /**
     * Carica i dati per il grafico a barre.
     * Calcola la distanza percorsa per ognuno degli ultimi 6 mesi.
     */
    private fun loadBarChartData() {
        // Evita crash se il context non è disponibile
        if (!isAdded) return

        val db = AppDatabase.getDatabase(requireContext())
        val dao = db.journeyLocationDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val entries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()
            val now = YearMonth.now()

            // Calcola i dati per gli ultimi 6 mesi
            for (i in 5 downTo 0) {
                val currentMonth = now.minusMonths(i.toLong())
                val monthLabel = currentMonth.month.name.substring(0, 3) // Es. "NOV"
                labels.add(monthLabel)

                // Calcola inizio e fine del mese in millisecondi
                val startOfMonth = currentMonth.atDay(1)
                    .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                val endOfMonth = currentMonth.plusMonths(1).atDay(1)
                    .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

                // Prendi le location per quel mese
                val locations = dao.getAllBetween(startOfMonth, endOfMonth)
                // Calcola i km (riutilizzando la tua funzione!)
                val totalKmThisMonth = calculateTotalKm(locations)

                // Aggiungi l'entrata al grafico (usiamo 5-i per avere l'ordine 0,1,2,3,4,5)
                entries.add(BarEntry((5 - i).toFloat(), totalKmThisMonth.toFloat()))
            }

            // Dati pronti, ora aggiorna la UI sul Main thread
            withContext(Dispatchers.Main) {
                // Assicurati che il fragment sia ancora "attaccato"
                if (isAdded) {
                    setupBarChart(entries)
                }
            }
        }
    }

    /**
     * Configura e popola il BarChart con i dati calcolati.
     */
    private fun setupBarChart(entries: ArrayList<BarEntry>) {
        val dataSet = BarDataSet(entries, "Km percorsi")
        // Usa un colore dai tuoi XML
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.purple_500)
        dataSet.setDrawValues(false) // Non mostrare i valori numerici sopra le barre

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        barChart.data = barData
        barChart.description.isEnabled = false // Nasconde la descrizione
        barChart.legend.isEnabled = false // Nasconde la legenda
        barChart.setFitBars(true) // Adatta le barre
        barChart.animateY(1000) // Animazione

        // Stile assi
        barChart.xAxis.isEnabled = false // Nasconde etichette asse X
        barChart.axisLeft.isEnabled = false // Nasconde asse Y sinistro
        barChart.axisRight.isEnabled = false // Nasconde asse Y destro
        barChart.setDrawGridBackground(false) // Niente griglia

        barChart.invalidate() // Ridisegna il grafico
    }
    // --- FINE BLOCCO DA AGGIUNGERE ---
}
