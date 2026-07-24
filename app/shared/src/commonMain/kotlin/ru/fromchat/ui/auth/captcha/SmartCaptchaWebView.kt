package ru.fromchat.ui.auth.captcha

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform WebView that loads Yandex SmartCaptcha and reports the verification token.
 */
@Composable
expect fun SmartCaptchaWebView(
    sitekey: String,
    languageTag: String,
    modifier: Modifier = Modifier,
    onToken: (String) -> Unit,
    onReady: () -> Unit = {},
    onChallengeVisible: () -> Unit = {},
    onChallengeHidden: () -> Unit = {},
    onError: (String) -> Unit = {},
)
