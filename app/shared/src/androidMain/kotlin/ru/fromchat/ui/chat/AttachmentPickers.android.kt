package ru.fromchat.ui.chat

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

actual fun getFilenameFromUri(uri: String): String {
    val context = com.pr0gramm3r101.utils.UtilsLibrary.context
    context.contentResolver.query(Uri.parse(uri), null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            val name = cursor.getString(nameIndex)
            if (!name.isNullOrBlank()) return name
        }
    }
    return uri.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "file"
}

@Composable
actual fun rememberImagePicker(onResult: (List<String>) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        onResult(uris.map { it.toString() })
    }
    return {
        launcher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
        )
    }
}

@Composable
actual fun rememberFilePicker(onResult: (List<String>) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        onResult(uris.map { it.toString() })
    }
    return {
        launcher.launch(arrayOf("*/*"))
    }
}
