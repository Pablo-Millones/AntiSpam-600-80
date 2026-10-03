package com.antispam.blocker.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BlockedCallsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("antispam_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_HISTORY = "key_history_list"
        private const val KEY_TOTAL_COUNT = "key_total_blocked_counter"
        private const val MAX_LOG_SIZE = 100
    }

    @Synchronized
    fun getBlockedCalls(): List<BlockedCall> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BlockedCall>>() {}.type
            gson.fromJson<List<BlockedCall>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addBlockedCall(phoneNumber: String, matchedPrefix: String) {
        val currentList = getBlockedCalls().toMutableList()
        val entry = BlockedCall(
            phoneNumber = phoneNumber,
            matchedPrefix = matchedPrefix
        )
        currentList.add(0, entry) // Añadir al inicio para que el más nuevo esté arriba

        // Mantener tamaño máximo
        val trimmed = if (currentList.size > MAX_LOG_SIZE) {
            currentList.subList(0, MAX_LOG_SIZE)
        } else {
            currentList
        }

        val json = gson.toJson(trimmed)
        val total = getTotalBlockedCount() + 1

        prefs.edit()
            .putString(KEY_HISTORY, json)
            .putInt(KEY_TOTAL_COUNT, total)
            .apply()
    }

    fun getTotalBlockedCount(): Int {
        return prefs.getInt(KEY_TOTAL_COUNT, 0)
    }

    @Synchronized
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    @Synchronized
    fun resetCounter() {
        prefs.edit().putInt(KEY_TOTAL_COUNT, 0).apply()
    }
}
