package com.example.travelcompanion.ui

import android.os.Bundle
import com.google.android.gms.maps.model.TileOverlayOptions
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
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
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
    private lateinit var barChart: BarChart
    private var currentMonthPoints: List<LatLng>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        totalKmText = view.findViewById(R.id.totalKmText)
        citiesVisitedText = view.findViewById(R.id.citiesVisitedText)
        barChart = view.findViewById(R.id.distanceBarChart)

        val mapFragment = childFragmentManager.findFragmentById(R.id.statsMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadStats()
        loadBarChartData()

        return view
    }

    private fun loadStats() {
        val db = AppDatabase.getDatabase(requireContext())
        val dao = db.journeyLocationDao()

        lifecycleScope.launch {
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val endOfMonth = now.plusMonths(1).withDayOfMonth(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()


            val (totalKm, cityCount, pathPoints) = withContext(Dispatchers.IO) {
                val locations = dao.getAllBetween(startOfMonth, endOfMonth)

                if (locations.isEmpty()) {
                    Triple(0.0, 0, emptyList<LatLng>())
                } else {
                    val km = calculateTotalKm(locations)
                    val cities = countCitiesVisited(locations)
                    val points = locations.map { LatLng(it.latitude, it.longitude) }
                    Triple(km, cities, points)
                }
            }

            // Aggiorna UI
            totalKmText.text = "Totale km: %.2f".format(totalKm)
            citiesVisitedText.text = "Città visitate: $cityCount"

            // Salva i punti. La mappa li disegnerà quando è pronta in onMapReady
            currentMonthPoints = pathPoints

            // Se la mappa è GIÀ pronta, disegna la heatmap ora
            if (map != null && pathPoints.isNotEmpty()) {
                addHeatMap(pathPoints)
            }

        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Quando la mappa è pronta, controlla se abbiamo già i dati
        // per disegnare la heatmap
        currentMonthPoints?.let { points ->
            if (points.isNotEmpty()) {
                addHeatMap(points)
            }
        }

    }

    private fun addHeatMap(points: List<LatLng>) {
        if (points.isEmpty()) return

        map?.clear() // Pulisce eventuali vecchie polyline
        // Converte LatLng in WeightedLatLng
        val weightedPoints = points.map { WeightedLatLng(it, 1.0) }

        // Crea il provider
        val provider = HeatmapTileProvider.Builder()
            .weightedData(weightedPoints)
            .radius(50) // Raggio (in pixel) di influenza di ogni punto
            .build()

        //  Crea le Opzioni e inserisci il provider
        val tileOverlayOptions = TileOverlayOptions()
            .tileProvider(provider)

        //Aggiungi le OPZIONI alla mappa, non il provider
        map?.addTileOverlay(tileOverlayOptions)

        // Centra la mappa sull'ultimo punto
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(points.last(), 10f))
    }


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
            // Questa funzione non dovrebbe più essere chiamata,
            // ma la lasciamo per sicurezza (verrà sovrascritta da addHeatMap)
        }
    }

    private fun loadBarChartData() {
        if (!isAdded) return

        val db = AppDatabase.getDatabase(requireContext())
        val dao = db.journeyLocationDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val entries = ArrayList<BarEntry>()
            val labels = ArrayList<String>() // Lista per le etichette (es. "Ago", "Set")
            val now = YearMonth.now()

            // Calcola i dati per gli ultimi 6 mesi
            for (i in 5 downTo 0) {
                val currentMonth = now.minusMonths(i.toLong())
                // Es. "NOV"
                val monthLabel = currentMonth.month.name.substring(0, 3)
                labels.add(monthLabel)

                // Calcola inizio e fine del mese in millisecondi
                val startOfMonth = currentMonth.atDay(1)
                    .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                val endOfMonth = currentMonth.plusMonths(1).atDay(1)
                    .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

                val locations = dao.getAllBetween(startOfMonth, endOfMonth)
                val totalKmThisMonth = calculateTotalKm(locations)

                // L'indice (5-i) deve corrispondere all'indice dell'etichetta
                entries.add(BarEntry((5 - i).toFloat(), totalKmThisMonth.toFloat()))
            }

            // Dati pronti, ora aggiorna la UI sul Main thread
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    // Ora passiamo anche le etichette alla funzione di setup
                    setupBarChart(entries, labels)
                }
            }
        }
    }

    private fun setupBarChart(entries: ArrayList<BarEntry>, labels: ArrayList<String>) {
        val dataSet = BarDataSet(entries, "Km percorsi")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.purple_500)

        // Mostra i valori numerici (i Km) sopra ogni barra
        dataSet.setDrawValues(true)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
        dataSet.valueTextSize = 10f
        // --- FINE MODIFICHE ---

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setFitBars(true)
        barChart.animateY(1000)

        // Configura l'asse X (i mesi)
        val xAxis = barChart.xAxis
        xAxis.isEnabled = true // Abilita l'asse X
        xAxis.position = XAxis.XAxisPosition.BOTTOM // Metti le etichette in basso
        xAxis.valueFormatter = IndexAxisValueFormatter(labels) // Usa i nomi dei mesi
        xAxis.setDrawGridLines(false) // Nasconde la griglia verticale
        xAxis.granularity = 1f // Mostra ogni etichetta

        // Configura l'asse Y (i Km)
        barChart.axisLeft.isEnabled = true // Abilita l'asse Y sinistro
        barChart.axisLeft.axisMinimum = 0f // Parti da 0 Km
        barChart.axisRight.isEnabled = false // Nascondi l'asse Y destro

        barChart.setDrawGridBackground(false)

        barChart.invalidate() // Ridisegna il grafico
    }
}