package ru.fromchat.ui.chat

import androidx.compose.runtime.Composable

actual fun getFilenameFromUri(uri: String): String {
    return uri.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "file"
}

@Composable
actual fun rememberImagePicker(onResult: (List<String>) -> Unit): () -> Unit {
    return { /* Phase 4: PHPickerViewController */ }
}

@Composable
actual fun rememberFilePicker(onResult: (List<String>) -> Unit): () -> Unit {
    return { /* Phase 4: UIDocumentPickerViewController */ }
}
