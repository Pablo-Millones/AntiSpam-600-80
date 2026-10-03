package com.antispam.blocker.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BlockedCall(
    val id: String = UUID.randomUUID().toString(),
    val phoneNumber: String,
    val matchedPrefix: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
