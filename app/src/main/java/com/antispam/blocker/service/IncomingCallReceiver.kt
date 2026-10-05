package com.antispam.blocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.antispam.blocker.data.BlockRuleManager
import com.antispam.blocker.data.BlockedCallsRepository
import com.antispam.blocker.util.PhoneNumberHelper

/**
 * Receptor de respaldo a nivel de sistema (BroadcastReceiver) para interceptar
 * llamadas entrantes de spam y cortarlas de inmediato vía TelecomManager.endCall(),
 * garantizando compatibilidad en capas de personalización como ColorOS/OxygenOS (OnePlus/Oppo)
 * donde CallScreeningService a veces es postergado por el sistema.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    private val tag = "IncomingCallReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) {
            return
        }

        val rawNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        Log.d(tag, "Llamada timbrando detectada vía PHONE_STATE. Número: '$rawNumber'")

        val ruleManager = BlockRuleManager(context)
        if (!ruleManager.isProtectionEnabled) {
            return
        }

        if (rawNumber.isBlank()) {
            return
        }

        val prefixes = ruleManager.getBlockedPrefixes()
        val matchResult = PhoneNumberHelper.checkIsBlocked(rawNumber, prefixes)

        if (matchResult.isBlocked) {
            Log.i(tag, "¡LLAMADA SPAM DETECTADA POR RECEIVER! Cortando llamada de: $rawNumber")

            // Cortar llamada inmediatamente vía TelecomManager
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS)
                    == PackageManager.PERMISSION_GRANTED) {
                    try {
                        val ended = telecomManager.endCall()
                        Log.i(tag, "telecomManager.endCall() resultado: $ended")
                    } catch (e: Exception) {
                        Log.e(tag, "Error al cortar llamada con TelecomManager", e)
                    }
                } else {
                    Log.w(tag, "Falta permiso ANSWER_PHONE_CALLS para colgar la llamada")
                }
            }

            // Registrar en historial local
            val prefixDetected = matchResult.matchedPrefix ?: "600"
            val repository = BlockedCallsRepository(context)
            repository.addBlockedCall(rawNumber, prefixDetected)
        }
    }
}
