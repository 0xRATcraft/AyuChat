package ru.fromchat.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.fromchat.api.AttachmentDownloadNotifier
import ru.fromchat.api.DmEnvelope
import ru.fromchat.api.DmFile

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatFileAttachmentTile(
    filename: String,
    sizeBytes: Long?,
    messageId: Int,
    fileIndex: Int,
    clientMessageId: String?,
    file: DmFile?,
    dmEnvelope: DmEnvelope?,
    currentUserId: Int?,
    pendingFileUri: String?,
    isAuthor: Boolean,
    isUploading: Boolean,
    uploadProgress: Int?,
    messageLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val isPendingLocal = pendingFileUri != null && file == null
    val mimeType = remember(filename) { mimeTypeForFilename(filename) }

    var exportUri by remember(messageId, fileIndex, clientMessageId) {
        mutableStateOf<String?>(null)
    }
    var uriAccessible by remember { mutableStateOf(false) }

    LaunchedEffect(messageId, fileIndex, clientMessageId) {
        val stored = DownloadedFileRegistry.getExportUri(messageId, fileIndex, clientMessageId)
        exportUri = stored
        uriAccessible = stored != null && isExportUriAccessible(stored)
        if (stored != null && !uriAccessible) {
            DownloadedFileRegistry.removeExportUri(messageId, fileIndex, clientMessageId)
            exportUri = null
            AttachmentDownloadNotifier.clearProgress(
                messageId = messageId,
                fileIndex = fileIndex,
                clientMessageId = clientMessageId,
                mirrorAsFileAttachment = true,
            )
        }
    }

    val downloadProgressByKey by AttachmentDownloadNotifier.progressPercentByKey.collectAsState()
    val downloadProgress = remember(downloadProgressByKey, messageId, fileIndex, clientMessageId) {
        DownloadedFileRegistry.resolveDownloadPercent(
            messageId = messageId,
            fileIndex = fileIndex,
            clientMessageId = clientMessageId,
            progressByKey = downloadProgressByKey,
        )
    }
    val isDownloading = !isUploading &&
        downloadProgress != null &&
        downloadProgress < 100

    var pendingDownload by remember { mutableStateOf<PendingFileDownload?>(null) }

    val launchDestinationPicker = rememberCreateDownloadDestinationLauncher { destination ->
        val pending = pendingDownload
        pendingDownload = null
        if (destination == null || pending == null) return@rememberCreateDownloadDestinationLauncher
        scope.launch {
            val ok = DmFileDownloader.downloadToExportUri(
                messageId = pending.messageId,
                fileIndex = pending.fileIndex,
                file = pending.file,
                envelope = pending.envelope,
                currentUserId = pending.currentUserId,
                clientMessageId = pending.clientMessageId,
                exportUri = destination,
                messageLabel = pending.messageLabel,
            )
            if (ok) {
                uriAccessible = isExportUriAccessible(destination)
                if (!uriAccessible) {
                    DownloadedFileRegistry.removeExportUri(
                        pending.messageId,
                        pending.fileIndex,
                        pending.clientMessageId,
                    )
                    exportUri = null
                }
            } else {
                exportUri = null
                uriAccessible = false
            }
        }
    }

    val isDownloaded = !isPendingLocal && uriAccessible && exportUri != null && !isDownloading
    val showWavy = isUploading || isDownloading

    val onRowClick: (() -> Unit)? = when {
        isUploading -> null
        isPendingLocal && pendingFileUri != null -> {
            {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        openExportUri(pendingFileUri, mimeType)
                    }
                }
            }
        }
        isDownloaded && exportUri != null -> {
            {
                scope.launch {
                    val accessible = isExportUriAccessible(exportUri!!)
                    if (!accessible) {
                        DownloadedFileRegistry.removeExportUri(messageId, fileIndex, clientMessageId)
                        exportUri = null
                        uriAccessible = false
                        AttachmentDownloadNotifier.clearProgress(
                            messageId = messageId,
                            fileIndex = fileIndex,
                            clientMessageId = clientMessageId,
                            mirrorAsFileAttachment = true,
                        )
                        return@launch
                    }
                    withContext(Dispatchers.Default) {
                        if (!openExportUri(exportUri!!, mimeType)) {
                            DownloadedFileRegistry.removeExportUri(messageId, fileIndex, clientMessageId)
                            exportUri = null
                            uriAccessible = false
                        }
                    }
                }
            }
        }
        file != null && dmEnvelope != null && !isDownloading -> {
            {
                pendingDownload = PendingFileDownload(
                    messageId = messageId,
                    fileIndex = fileIndex,
                    file = file,
                    envelope = dmEnvelope,
                    currentUserId = currentUserId,
                    clientMessageId = clientMessageId,
                    messageLabel = messageLabel,
                )
                launchDestinationPicker(filename, mimeType)
            }
        }
        else -> null
    }

  ExpressiveFileAttachmentRow(
        filename = filename,
        sizeBytes = sizeBytes,
        onClick = onRowClick,
        isAuthor = isAuthor,
        isUploading = showWavy,
        uploadProgress = when {
            isUploading -> uploadProgress
            isDownloading -> downloadProgress
            else -> null
        },
        isDownloaded = isDownloaded,
        modifier = modifier,
    )
}

private data class PendingFileDownload(
    val messageId: Int,
    val fileIndex: Int,
    val file: DmFile,
    val envelope: DmEnvelope,
    val currentUserId: Int?,
    val clientMessageId: String?,
    val messageLabel: String?,
)
