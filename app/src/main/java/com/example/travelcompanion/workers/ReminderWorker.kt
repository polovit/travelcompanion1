package com.example.travelcompanion.workers // Assicurati che il package sia corretto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.travelcompanion.MainActivity
import com.example.travelcompanion.R
import com.example.travelcompanion.model.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.util.Log // Aggiungi questo import in cima
class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val db = AppDatabase.getDatabase(appContext)
    private val tripDao = db.tripDao()

    companion object {
        const val WORK_NAME = "TripReminderWorker"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "reminder_channel"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("ReminderWorker", "WORKER PARTITO!") // Aggiungi questa riga
            // Logica del worker:
            // 1. Prendi l'ultimo viaggio dal DAO
            val lastTrip = tripDao.getLatestTrip() // Dovremo aggiungere questa funzione al DAO

            // 2. Calcola la data di 7 giorni fa
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val sevenDaysAgo = calendar.timeInMillis

            var shouldNotify = true

            // 3. Controlla se dobbiamo notificare
            if (lastTrip != null) {
                // Prova a parsare la data di inizio del viaggio
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                try {
                    val lastTripDate = sdf.parse(lastTrip.startDate ?: "")?.time ?: 0
                    if (lastTripDate > sevenDaysAgo) {
                        shouldNotify = false // L'utente ha fatto un viaggio di recente
                    }
                } catch (e: Exception) {
                    // Ignora l'errore di parsing, per sicurezza notifichiamo
                }
            }

            // 4. Se dobbiamo notificare, invia la notifica
            if (shouldNotify) {
                sendNotification()
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun sendNotification() {
        val context = applicationContext
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Canale di notifica (necessario da Android 8+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Promemoria Viaggi",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Promemoria per registrare nuovi viaggi"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent per aprire l'app quando si clicca la notifica
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Costruisci la notifica
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trips) // Usa l'icona che abbiamo corretto!
            .setContentTitle("È ora di una nuova avventura?")
            .setContentText("È un po' che non registri un viaggio. Tocca per aprire l'app.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}