package com.example.travelcompanion

import android.app.Application
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.travelcompanion.workers.ActivityDetectionReceiver
import com.example.travelcompanion.workers.ReminderWorker
import com.google.android.gms.location.ActivityRecognition
import java.util.concurrent.TimeUnit

class TravelCompanionApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Schedula il worker periodico (che hai già fatto)
        setupPeriodicWork()

        // --- RIGA DA AGGIUNGERE ---
        // Avvia il rilevamento dell'attività
        startActivityDetection()
        // --- FINE RIGA ---
    }

    private fun setupPeriodicWork() {
        // Codice originale per il ReminderWorker (ogni 1 giorno)
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )

        Log.d("TravelCompanionApp", "Worker periodico (1 giorno) schedulato.")
    }

    // --- BLOCCO INTERO DA AGGIUNGERE ---
    /**
     * Avvia il client di ActivityRecognition per ascoltare
     * le attività dell'utente in background.
     */
    @SuppressLint("MissingPermission") // Il permesso viene chiesto nella MainActivity
    private fun startActivityDetection() {
        val context = this.applicationContext

        // Questo Intent punta al nostro Receiver
        val intent = Intent(context, ActivityDetectionReceiver::class.java)
        intent.action = "com.example.travelcompanion.ACTIVITY_DETECTED" // L'azione che abbiamo definito nel manifest

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, // Request code 0 per questo
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intervallo di rilevamento: ogni 5 minuti (300_000 millisecondi)
        // È un buon equilibrio tra reattività e batteria
        val detectionInterval: Long = 300_000

        ActivityRecognition.getClient(context)
            .requestActivityUpdates(detectionInterval, pendingIntent)
            .addOnSuccessListener {
                Log.d("TravelCompanionApp", "Rilevamento attività avviato correttamente.")
            }
            .addOnFailureListener { e ->
                Log.e("TravelCompanionApp", "Rilevamento attività fallito: $e")
            }
    }
    // --- FINE BLOCCO DA AGGIUNGERE ---
}