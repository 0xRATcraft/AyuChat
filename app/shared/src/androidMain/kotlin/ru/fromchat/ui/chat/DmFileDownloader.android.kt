package ru.fromchat.ui.chat

import android.content.Intent
import android.net.Uri
import com.pr0gramm3r101.utils.UtilsLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun writeBytesToExportUri(exportUri: String, bytes: ByteArray): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(exportUri)
            UtilsLibrary.context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(bytes)
            } != null
        }.getOrDefault(false)
    }

actual suspend fun isExportUriAccessible(exportUri: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        when {
            exportUri.startsWith("content://") -> {
                UtilsLibrary.context.contentResolver
                    .openFileDescriptor(Uri.parse(exportUri), "r")
                    ?.use { true } == true
            }
            exportUri.startsWith("file://") -> {
                val path = Uri.parse(exportUri).path ?: return@runCatching false
                File(path).isFile
            }
            else -> File(exportUri).isFile
        }
    }.getOrDefault(false)
}

actual fun openExportUri(exportUri: String, mimeType: String): Boolean {
    val context = UtilsLibrary.context
    val uri = Uri.parse(
        when {
            exportUri.startsWith("content://") || exportUri.startsWith("file://") -> exportUri
            else -> "file://$exportUri"
        },
    )
    if (uri.scheme == "file") {
        val path = uri.path ?: return false
        if (!File(path).isFile) return false
    }
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, null))
        true
    }.getOrDefault(false)
}
