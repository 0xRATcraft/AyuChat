package ru.fromchat.ui.auth

import ru.fromchat.api.schema.user.auth.SmartCaptchaParams
import ru.fromchat.api.schema.user.auth.YandexOAuthParams

/**
 * Survives [AuthScreen] leaving composition when navigating to Yandex OAuth / SmartCaptcha routes.
 * Cleared on welcome / successful auth / explicit reset to username.
 */
internal object AuthRegisterDraft {
    var username: String = ""
    var password: String = ""
    var confirmPassword: String = ""
    var displayName: String = ""
    var bio: String = ""
    var yandexRequired: Boolean = false
    var yandexParams: YandexOAuthParams? = null
    var captchaRequired: Boolean = false
    var captchaParams: SmartCaptchaParams? = null
    var registrationProof: String? = null
    var captchaToken: String? = null
    var page: Int = 0

    fun clear() {
        username = ""
        password = ""
        confirmPassword = ""
        displayName = ""
        bio = ""
        yandexRequired = false
        yandexParams = null
        captchaRequired = false
        captchaParams = null
        registrationProof = null
        captchaToken = null
        page = 0
    }
}
