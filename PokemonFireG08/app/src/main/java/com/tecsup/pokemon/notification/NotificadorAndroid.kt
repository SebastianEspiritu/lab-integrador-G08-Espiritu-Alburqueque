package com.tecsup.pokemon.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tecsup.pokemon.MainActivity
import com.tecsup.pokemon.R

/**
 * Implementación real del Notificador. El ViewModel solo conoce la interface,
 * así nunca recibe un Context.
 */
class NotificadorAndroid(context: Context) : Notificador {
    private val appContext = context.applicationContext

    init {
        crearCanal()
    }

    @SuppressLint("MissingPermission") // El permiso se revisa en puedeNotificar().
    override fun mostrar(titulo: String, texto: String) {
        // PA-14: si el permiso está negado, la app sigue normal y simplemente no avisa.
        if (!puedeNotificar()) return

        // Al tocar la notificación se abre la app.
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val alTocar = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(appContext, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(alTocar)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICACION_ID, notificacion)
        } catch (e: SecurityException) {
            // El usuario quitó el permiso justo ahora: no se muestra nada.
        }
    }

    private fun puedeNotificar(): Boolean {
        val permisoConcedido = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permisoConcedido && NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Sincronización",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisa cuántos Pokémon se guardaron en el celular." }
            appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private companion object {
        const val CANAL_ID = "sincronizacion"
        const val NOTIFICACION_ID = 1001
    }
}
