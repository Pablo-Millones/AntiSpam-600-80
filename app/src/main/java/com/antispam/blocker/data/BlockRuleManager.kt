package com.antispam.blocker.data

import android.content.Context
import android.content.SharedPreferences

class BlockRuleManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("antispam_rules_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROTECTION_ENABLED = "key_protection_enabled"
        private const val KEY_PREFIXES = "key_prefixes"
        private const val KEY_REJECT_CALL = "key_reject_call"
        private const val KEY_KEEP_IN_LOG = "key_keep_in_log"
        private const val KEY_SILENT_NOTIFICATION = "key_silent_notification"

        // Prefijos por defecto solicitados: 600 y los que empiezan en 80 (cubre 800, 801, 80, etc.)
        val DEFAULT_PREFIXES = listOf("600", "80")
    }

    var isProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()

    var rejectCallImmediately: Boolean
        get() = prefs.getBoolean(KEY_REJECT_CALL, true)
        set(value) = prefs.edit().putBoolean(KEY_REJECT_CALL, value).apply()

    var keepInSystemCallLog: Boolean
        get() = prefs.getBoolean(KEY_KEEP_IN_LOG, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_IN_LOG, value).apply()

    var silentNotification: Boolean
        get() = prefs.getBoolean(KEY_SILENT_NOTIFICATION, true)
        set(value) = prefs.edit().putBoolean(KEY_SILENT_NOTIFICATION, value).apply()

    fun getBlockedPrefixes(): List<String> {
        val set = prefs.getStringSet(KEY_PREFIXES, null)
        return if (set != null && set.isNotEmpty()) {
            set.toList().sorted()
        } else {
            DEFAULT_PREFIXES
        }
    }

    fun addPrefix(newPrefix: String): Boolean {
        val clean = newPrefix.trim().replace(Regex("[^0-9]"), "")
        if (clean.isBlank()) return false

        val current = getBlockedPrefixes().toMutableSet()
        val added = current.add(clean)
        if (added) {
            prefs.edit().putStringSet(KEY_PREFIXES, current).apply()
        }
        return added
    }

    fun removePrefix(prefix: String): Boolean {
        val current = getBlockedPrefixes().toMutableSet()
        val removed = current.remove(prefix)
        if (removed) {
            prefs.edit().putStringSet(KEY_PREFIXES, current).apply()
        }
        return removed
    }

    fun restoreDefaults() {
        prefs.edit().putStringSet(KEY_PREFIXES, DEFAULT_PREFIXES.toSet()).apply()
        isProtectionEnabled = true
        rejectCallImmediately = true
        keepInSystemCallLog = true
        silentNotification = true
    }
}
