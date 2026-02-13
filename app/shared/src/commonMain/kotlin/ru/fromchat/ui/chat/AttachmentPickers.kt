package ru.fromchat.ui.chat

import androidx.compose.runtime.Composable

data class SelectedAttachment(
    val id: String,
    val uri: String,
    val filename: String,
    val sizeBytes: Long?,
    val isImage: Boolean
)

/**
 * Platform-specific image picker. Returns a launch function.
 * On Android: PickVisualMedia (images only, multiple).
 * On iOS: Placeholder (no-op until Phase 4).
 */
@Composable
expect fun rememberImagePicker(onResult: (List<String>) -> Unit): () -> Unit

/**
 * Platform-specific file picker. Returns a launch function.
 * On Android: OpenDocument/OpenMultipleDocuments (SAF).
 * On iOS: Placeholder (no-op until Phase 4).
 */
@Composable
expect fun rememberFilePicker(onResult: (List<String>) -> Unit): () -> Unit

/** Resolve display filename from content URI. Platform-specific. */
expect fun getFilenameFromUri(uri: String): String
