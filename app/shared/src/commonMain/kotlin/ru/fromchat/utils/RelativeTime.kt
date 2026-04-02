package ru.fromchat.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Returns a user-friendly string for online status / last seen from an ISO-8601 timestamp.
 *
 * Online users show "Online". Offline users show a 24-hour time in the user's local timezone:
 * - Same-day timestamps: "Last seen HH:mm"
 * - Older timestamps: "Last seen YYYY-MM-DD HH:mm"
 */
fun formatLastSeen(online: Boolean, lastSeenIso: String?): String {
    if (online) return "Online"
    val iso = lastSeenIso ?: return ""

    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return "Last seen $iso"

    val timeZone = TimeZone.currentSystemDefault()
    val lastLocal = instant.toLocalDateTime(timeZone)

    val year = lastLocal.date.year
    val month = lastLocal.date.monthNumber.toString().padStart(2, '0')
    val day = lastLocal.date.dayOfMonth.toString().padStart(2, '0')
    val hour = lastLocal.hour.toString().padStart(2, '0')
    val minute = lastLocal.minute.toString().padStart(2, '0')
    val timePart = "$hour:$minute"

    return "Last seen $year-$month-$day $timePart"
}

