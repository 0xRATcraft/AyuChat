package ru.fromchat.ui.auth.captcha

import androidx.compose.runtime.saveable.listSaver
import kotlin.concurrent.Volatile

/**
 * Root [androidx.navigation.NavController] route for SmartCaptcha.
 * Client key is staged in [pending] before navigate (not embedded in the route).
 */
internal object SmartCaptchaNav {
    const val ROUTE = "smartCaptcha"
    const val RESULT_TOKEN = "smartcaptcha_token"
    const val RESULT_ERROR = "smartcaptcha_error"

    data class Session(
        val clientKey: String,
    )

    val SessionSaver = listSaver<Session?, String>(
        save = { session ->
            if (session == null) emptyList()
            else listOf(session.clientKey)
        },
        restore = { saved ->
            if (saved.isEmpty()) null
            else Session(clientKey = saved[0])
        },
    )

    @Volatile
    var pending: Session? = null
}
