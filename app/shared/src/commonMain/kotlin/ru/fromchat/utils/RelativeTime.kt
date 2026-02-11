package ru.fromchat.utils

import kotlinx.datetime.Instant
import kotlin.time.Clock

/**
 * Returns a user-friendly string for online status / last seen from an ISO-8601 timestamp.
 * E.g. "Online", "Last seen just now", "Last seen 5 min ago", "Last seen yesterday".
 */
fun formatLastSeen(online: Boolean, lastSeenIso: String?): String {
    if (online) return "Online"
    val iso = lastSeenIso ?: return ""
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return "Last seen $iso"
    val nowSeconds = Clock.System.now().toEpochMilliseconds() / 1000L
    val pastSeconds = instant.epochSeconds
    val diffSeconds = nowSeconds - pastSeconds
    val diffMinutes = diffSeconds / 60L
    val diffHours = diffSeconds / 3600L
    val diffDays = diffSeconds / 86400L
    val relative = when {
        diffMinutes < 1L -> "just now"
        diffMinutes < 60L -> if (diffMinutes == 1L) "1 min ago" else "$diffMinutes min ago"
        diffHours < 24L -> if (diffHours == 1L) "1 hour ago" else "$diffHours hours ago"
        diffDays < 2L -> "yesterday"
        else -> {
            val parts = iso.split("T").firstOrNull()?.split("-") ?: return "Last seen $iso"
            if (parts.size == 3) {
                val (_, m, d) = parts
                val month = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").getOrNull(m.toIntOrNull() ?: 0) ?: m
                "$month $d"
            } else "Last seen $iso"
        }
    }
    return "Last seen $relative"
}
