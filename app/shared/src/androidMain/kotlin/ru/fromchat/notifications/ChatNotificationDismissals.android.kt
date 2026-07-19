package ru.fromchat.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import ru.fromchat.Logger

actual object ChatNotificationDismissals {
    /** Must match [ru.fromchat.notifications.NotificationHelper] summary id in app:android. */
    private const val SUMMARY_NOTIFICATION_ID = 1_000_000

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
    }

    actual fun dismissAllMessageNotifications() {
        val context = appContext ?: return
        runCatching {
            NotificationManagerCompat.from(context).cancel(SUMMARY_NOTIFICATION_ID)
            Logger.d("ChatNotificationDismissals", "Cancelled message notifications")
        }.onFailure {
            Logger.w("ChatNotificationDismissals", "Failed to cancel notifications: ${it.message}", it)
        }
    }
}
