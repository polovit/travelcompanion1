package com.example.travelcompanion

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.travelcompanion.workers.ReminderWorker
import java.util.concurrent.TimeUnit
import androidx.work.OneTimeWorkRequestBuilder // <-- ASSICURATI DI USARE QUESTO
class TravelCompanionApp : Application() {

    override fun onCreate() {
        super.onCreate()

        setupPeriodicWork()
    }

    private fun setupPeriodicWork() {

        // --- QUESTO È IL CODICE DI PRODUZIONE ---
        // 1. Crea una richiesta periodica (ogni 1 giorno)
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .build()

        // 2. Schedula il lavoro in modo unico
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Mantiene il vecchio lavoro se già attivo
            reminderRequest
        )

        Log.d("TravelCompanionApp", "Worker periodico (1 giorno) schedulato.")
        // --- FINE CODICE DI PRODUZIONE ---
    }
}