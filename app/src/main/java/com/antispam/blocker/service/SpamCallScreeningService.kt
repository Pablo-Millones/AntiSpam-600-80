package com.antispam.blocker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antispam.blocker.R
import com.antispam.blocker.data.BlockRuleManager
import com.antispam.blocker.data.BlockedCallsRepository
import com.antispam.blocker.util.PhoneNumberHelper

class SpamCallScreeningService : CallScreeningService() {

    private val tag = "SpamCallScreening"
    private val channelId = "spam_blocked_calls_channel"

    override fun onScreenCall(callDetails: Call.Details) {
        val ruleManager = BlockRuleManager(this)
        val repository = BlockedCallsRepository(this)
        val spamDb = com.antispam.blocker.data.SpamDatabaseManager.getInstance(this)

        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        val direction = callDetails.callDirection

        Log.d(tag, "Llamada entrante detectada. Número: '$rawNumber', Dirección: $direction")

        // Solo evaluamos llamadas entrantes
        if (direction != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Si la protección global está desactivada, permitir llamada
        if (!ruleManager.isProtectionEnabled) {
            Log.d(tag, "Protección desactivada. Permitiendo llamada de: $rawNumber")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val prefixes = ruleManager.getBlockedPrefixes()
        val matchResult = PhoneNumberHelper.checkIsBlocked(rawNumber, prefixes, spamDb)

        if (matchResult.isBlocked) {
            Log.i(tag, "¡LLAMADA BLOQUEADA! Número: $rawNumber, Prefijo: ${matchResult.matchedPrefix}")

            // Construir respuesta de bloqueo
            val response = CallResponse.Builder()
                .setDisallowCall(true) // No permitir timbrado normal
                .setRejectCall(ruleManager.rejectCallImmediately) // Colgar automáticamente
                .setSkipCallLog(!ruleManager.keepInSystemCallLog) // Decidir si se registra en historial del teléfono
                .setSkipNotification(ruleManager.silentNotification) // Silenciar llamada entrante
                .build()

            respondToCall(callDetails, response)

            // Registrar en el historial local de la app
            val prefixDetected = matchResult.matchedPrefix ?: "Desconocido"
            repository.addBlockedCall(rawNumber, prefixDetected)

            // Emitir notificación al usuario informando del bloqueo silencioso
            notifyCallBlocked(rawNumber, prefixDetected)
        } else {
            Log.d(tag, "Llamada permitida de: $rawNumber (${matchResult.reason})")
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }

    private fun notifyCallBlocked(phoneNumber: String, prefix: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Llamada Spam Bloqueada")
            .setContentText("Se rechazó llamada de: $phoneNumber (Prefijo: $prefix)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
