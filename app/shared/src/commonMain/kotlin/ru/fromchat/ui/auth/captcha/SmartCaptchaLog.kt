package ru.fromchat.ui.auth.captcha

/** Safe summaries for SmartCaptcha logs (never dump full tokens/secrets). */
internal object SmartCaptchaLog {
    const val TAG = "SmartCaptcha"

    fun redactKey(value: String?): String {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return "(empty)"
        if (v.length <= 8) return "len=${v.length}"
        return "len=${v.length} prefix=${v.take(4)}…suffix=${v.takeLast(4)}"
    }

    fun redactToken(value: String?): String {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return "(empty)"
        return "len=${v.length} prefix=${v.take(6)}…"
    }

    fun shortUrl(url: String?): String {
        if (url.isNullOrBlank()) return "null"
        // Drop query sitekey from logs; keep path/host.
        val q = url.indexOf('?')
        val base = if (q >= 0) url.substring(0, q) else url
        return if (base.length <= 120) base else base.take(117) + "..."
    }
}
