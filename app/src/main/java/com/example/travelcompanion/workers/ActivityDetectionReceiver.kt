package com.example.travelcompanion.workers // Assicurati che il nome del package sia corretto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.travelcompanion.R
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityDetectionReceiver : BroadcastReceiver() {

    companion object {
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "activity_channel"
        private const val TAG = "ActivityDetection"
        // Flag per evitare spam di notifiche (opzionale ma consigliato)
        private var notificationSent = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) {
            return
        }

        val result = ActivityRecognitionResult.extractResult(intent)
        result?.let { handleDetectedActivities(it.probableActivities, context) }
    }

    private fun handleDetectedActivities(activities: List<DetectedActivity>, context: Context) {
        // Controlla le attività più probabili
        for (activity in activities) {
            val activityType = activity.type
            val confidence = activity.confidence

            // Se l'utente è in un veicolo con alta probabilità E non abbiamo già inviato una notifica
            if ((activityType == DetectedActivity.IN_VEHICLE) && confidence >= 75) {
                if (!notificationSent) {
                    Log.d(TAG, "Attività rilevata: IN_VEHICLE (Confidenza: $confidence%)")
                    sendNotification(context)
                    notificationSent = true // Imposta il flag per non inviare più notifiche
                }
                break // Abbiamo trovato quello che ci serve
            }

            // Resetta il flag se l'utente è fermo (così riceverà la notifica al prossimo viaggio)
            if (activityType == DetectedActivity.STILL && confidence >= 75) {
                Log.d(TAG, "Attività rilevata: STILL. Resetto il flag notifiche.")
                notificationSent = false
            }
        }
    }

    /**
     * Crea e invia la notifica.
     * Cliccando, l'utente viene portato alla MainActivity.
     */
    private fun sendNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Canale (necessario da Android 8+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rilevamento Attività",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // Intent per aprire l'app quando l'utente clicca la notifica
        val openAppIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)

        val pendingIntent = PendingIntent.getActivity(
            context,
            1, // Request code diverso dal primo (0)
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Costruisci la notifica
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location) // Usa l'icona della posizione
            .setContentTitle("Sei in viaggio?")
            .setContentText("Sembra che tu ti stia muovendo. Vuoi registrare il viaggio?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Azione al click
            .setAutoCancel(true) // La notifica sparisce dopo il click
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}