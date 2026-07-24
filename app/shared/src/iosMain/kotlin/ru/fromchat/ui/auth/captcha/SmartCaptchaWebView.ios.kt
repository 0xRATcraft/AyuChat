package ru.fromchat.ui.auth.captcha

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import ru.fromchat.Logger

@Composable
actual fun SmartCaptchaWebView(
    sitekey: String,
    languageTag: String,
    modifier: Modifier,
    onToken: (String) -> Unit,
    onReady: () -> Unit,
    onChallengeVisible: () -> Unit,
    onChallengeHidden: () -> Unit,
    onError: (String) -> Unit,
) {
    LaunchedEffect(sitekey) {
        Logger.w(
            SmartCaptchaLog.TAG,
            "iOS stub: captcha unavailable sitekey=${SmartCaptchaLog.redactKey(sitekey)} " +
                "languageTag=$languageTag",
        )
        onError("Captcha is not available on this platform yet.")
    }
}
