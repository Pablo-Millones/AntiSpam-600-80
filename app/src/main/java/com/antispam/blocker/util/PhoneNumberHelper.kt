package com.antispam.blocker.util

data class NumberMatchResult(
    val isBlocked: Boolean,
    val matchedPrefix: String? = null,
    val normalizedNumber: String,
    val reason: String
)

object PhoneNumberHelper {

    /**
     * Limpia y normaliza un número telefónico removiendo espacios, guiones,
     * paréntesis y prefijos internacionales comunes (+56 para Chile u otros países).
     */
    fun normalize(rawNumber: String): String {
        if (rawNumber.isBlank()) return ""

        // Extraer únicamente dígitos y '+' inicial si existe
        var cleaned = rawNumber.trim().replace(Regex("[^0-9+]"), "")

        // Si empieza con '+', remover el signo '+'
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1)
        }

        // Remoción de código de país común (ejemplo Chile +56) si viene en formato internacional
        // +56 600... -> 56600...
        if (cleaned.startsWith("56") && cleaned.length >= 6) {
            cleaned = cleaned.substring(2)
        }

        // Si empieza con 0 inicial (ej: 0600 o 080), remover el 0
        if (cleaned.startsWith("0") && cleaned.length > 3) {
            cleaned = cleaned.substring(1)
        }

        return cleaned
    }

    /**
     * Evalúa si un número coincide con alguna de las reglas de prefijos configuradas.
     * Ejemplo de prefijos: "600", "80" (que también cubre 800, 801, etc.)
     */
    fun checkIsBlocked(rawNumber: String, blockedPrefixes: List<String>): NumberMatchResult {
        if (rawNumber.isBlank()) {
            return NumberMatchResult(
                isBlocked = false,
                normalizedNumber = "",
                reason = "Número vacío o desconocido"
            )
        }

        val digitsOnly = rawNumber.replace(Regex("[^0-9]"), "")
        val normalized = normalize(rawNumber)

        for (prefix in blockedPrefixes) {
            val cleanPrefix = prefix.trim().replace(Regex("[^0-9]"), "")
            if (cleanPrefix.isBlank()) continue

            // 1. Verificar coincidencia directa en el número normalizado
            if (normalized.startsWith(cleanPrefix)) {
                return NumberMatchResult(
                    isBlocked = true,
                    matchedPrefix = cleanPrefix,
                    normalizedNumber = normalized,
                    reason = "Comienza con el prefijo bloqueado '$cleanPrefix'"
                )
            }

            // 2. Verificar también en la cadena completa de dígitos (directo, con 56 o con prefijo internacional +1 para fraudes tipo 809/800)
            if (digitsOnly.startsWith(cleanPrefix) ||
                digitsOnly.startsWith("56$cleanPrefix") ||
                (digitsOnly.startsWith("1$cleanPrefix") && (cleanPrefix.startsWith("80") || cleanPrefix.startsWith("809")))) {
                return NumberMatchResult(
                    isBlocked = true,
                    matchedPrefix = cleanPrefix,
                    normalizedNumber = normalized,
                    reason = "Comienza con el prefijo bloqueado '$cleanPrefix'"
                )
            }
        }

        return NumberMatchResult(
            isBlocked = false,
            normalizedNumber = normalized,
            reason = "No coincide con ningún prefijo de spam activo"
        )
    }
}
