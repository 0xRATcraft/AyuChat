package ru.fromchat.notifications

/** Platform bridge so shared mark-read can dismiss message notifications. */
expect object ChatNotificationDismissals {
    fun dismissAllMessageNotifications()
}
