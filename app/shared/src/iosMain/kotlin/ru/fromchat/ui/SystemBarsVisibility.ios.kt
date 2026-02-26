package ru.fromchat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSystemBarsController(): ((Boolean) -> Unit)? {
    return remember {
        { _ -> /* iOS 13+ uses scene-based API; status bar control requires native Swift/ObjC bridge */ }
    }
}
