package com.antispam.blocker.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.antispam.blocker.util.PhoneNumberHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class SpamNumberEntry(
    val raw: String,
    val norm: String,
    val name: String,
    val isRegex: Boolean = false
)

class SpamDatabaseManager(private val context: Context) {

    private val tag = "SpamDatabaseManager"
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("spam_db_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val cacheFile = File(context.applicationContext.filesDir, "spam_database.json")
    private val customFile = File(context.applicationContext.filesDir, "custom_spam_numbers.json")

    // Mapas en memoria para búsqueda instantánea O(1)
    private val exactEntries = ConcurrentHashMap<String, SpamNumberEntry>()
    private val regexEntries = mutableListOf<Pair<Regex, SpamNumberEntry>>()
    private val customEntries = ConcurrentHashMap<String, SpamNumberEntry>()

    companion object {
        private const val KEY_DB_ENABLED = "key_db_filter_enabled"
        private const val KEY_LAST_SYNC = "key_last_sync_timestamp"
        private const val KEY_DB_VERSION = "key_db_version"
        private const val REMOTE_CSV_URL = "https://raw.githubusercontent.com/racuna/SpamChile/main/ChileSpam.csv"

        @Volatile
        private var instance: SpamDatabaseManager? = null

        fun getInstance(context: Context): SpamDatabaseManager {
            return instance ?: synchronized(this) {
                instance ?: SpamDatabaseManager(context).also { instance = it }
            }
        }
    }

    init {
        loadDatabase()
        loadCustomNumbers()
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_DB_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DB_ENABLED, value).apply()

    val lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)

    val totalCount: Int
        get() = exactEntries.size + regexEntries.size

    val customCount: Int
        get() = customEntries.size

    /**
     * Carga inicial desde caché local o desde assets empaquetados en el APK
     */
    @Synchronized
    private fun loadDatabase() {
        try {
            exactEntries.clear()
            regexEntries.clear()

            val jsonString = if (cacheFile.exists() && cacheFile.length() > 0) {
                cacheFile.readText(Charsets.UTF_8)
            } else {
                context.assets.open("spam_numbers_chile.json").use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).readText()
                }
            }

            val type = object : TypeToken<List<SpamNumberEntry>>() {}.type
            val list: List<SpamNumberEntry> = gson.fromJson(jsonString, type) ?: emptyList()

            for (entry in list) {
                indexEntry(entry)
            }
            Log.d(tag, "Base de datos cargada: ${exactEntries.size} números exactos, ${regexEntries.size} regex.")
        } catch (e: Exception) {
            Log.e(tag, "Error al cargar base de datos spam", e)
        }
    }

    @Synchronized
    private fun loadCustomNumbers() {
        try {
            customEntries.clear()
            if (customFile.exists() && customFile.length() > 0) {
                val json = customFile.readText(Charsets.UTF_8)
                val type = object : TypeToken<List<SpamNumberEntry>>() {}.type
                val list: List<SpamNumberEntry> = gson.fromJson(json, type) ?: emptyList()
                for (item in list) {
                    customEntries[item.norm] = item
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error al cargar números personalizados", e)
        }
    }

    private fun indexEntry(entry: SpamNumberEntry) {
        if (entry.isRegex) {
            try {
                val pattern = Regex(entry.raw.replace("+", "\\+"))
                regexEntries.add(pattern to entry)
            } catch (e: Exception) {
                // Si la regex es inválida, indexar por prefijo numérico
                exactEntries[entry.norm] = entry
            }
        } else {
            exactEntries[entry.norm] = entry
        }
    }

    /**
     * Evalúa si un número telefónico coincide con algún número registrado de spam (+569, +5644, etc.)
     */
    fun isSpam(rawNumber: String): SpamNumberEntry? {
        if (!isEnabled || rawNumber.isBlank()) return null

        val norm = PhoneNumberHelper.normalize(rawNumber)
        val digitsOnly = rawNumber.replace(Regex("[^0-9]"), "")

        // 1. Coincidencia en números personalizados por el usuario
        customEntries[norm]?.let { return it }
        if (digitsOnly != norm) {
            customEntries[digitsOnly]?.let { return it }
        }

        // 2. Coincidencia exacta O(1) en la base de datos comunitaria
        exactEntries[norm]?.let { return it }
        if (digitsOnly != norm) {
            exactEntries[digitsOnly]?.let { return it }
        }

        // 3. Coincidencia por expresión regular o patrón de rango (+569554000.*)
        for ((regex, entry) in regexEntries) {
            if (regex.containsMatchIn(rawNumber) || regex.containsMatchIn(norm) || regex.containsMatchIn(digitsOnly)) {
                return entry
            }
        }

        return null
    }

    /**
     * Agrega un número manual personalizado a la lista negra
     */
    @Synchronized
    fun addCustomNumber(number: String, name: String = "Número bloqueado"): Boolean {
        val norm = PhoneNumberHelper.normalize(number)
        if (norm.isBlank()) return false

        val entry = SpamNumberEntry(
            raw = number.trim(),
            norm = norm,
            name = if (name.isNotBlank()) name.trim() else "Número bloqueado",
            isRegex = false
        )
        customEntries[norm] = entry
        saveCustomNumbers()
        return true
    }

    /**
     * Elimina un número personalizado
     */
    @Synchronized
    fun removeCustomNumber(number: String): Boolean {
        val norm = PhoneNumberHelper.normalize(number)
        val removed = customEntries.remove(norm) != null
        if (removed) {
            saveCustomNumbers()
        }
        return removed
    }

    fun getCustomList(): List<SpamNumberEntry> {
        return customEntries.values.toList().sortedBy { it.raw }
    }

    private fun saveCustomNumbers() {
        try {
            val json = gson.toJson(customEntries.values.toList())
            customFile.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(tag, "Error guardando números personalizados", e)
        }
    }

    /**
     * Sincroniza y descarga la última versión comunitaria de ChileSpam desde GitHub
     */
    suspend fun syncWithRemote(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL(REMOTE_CSV_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "anti-spam-android")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("Error de conexión: código HTTP ${connection.responseCode}"))
            }

            val entries = mutableListOf<SpamNumberEntry>()
            connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("+56") || trimmed.startsWith("56")) {
                        val parts = trimmed.split(",")
                        val raw = parts[0].trim()
                        val name = if (parts.size > 1 && parts[1].isNotBlank()) parts[1].trim() else "Spam reportado"

                        var norm = raw.replace(Regex("[^0-9]"), "")
                        if (norm.startsWith("56") && norm.length >= 6) {
                            norm = norm.substring(2)
                        }

                        val isRegex = raw.contains(".*") || raw.contains("*")
                        entries.add(SpamNumberEntry(raw, norm, name, isRegex))
                    }
                }
            }

            if (entries.isNotEmpty()) {
                val json = gson.toJson(entries)
                cacheFile.writeText(json, Charsets.UTF_8)
                prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()

                // Recargar en memoria
                loadDatabase()
                Result.success(entries.size)
            } else {
                Result.failure(Exception("El archivo descargado no contenía números válidos"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Fallo al sincronizar base de datos con GitHub", e)
            Result.failure(e)
        }
    }
}
