package ru.fromchat.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import ru.fromchat.api.ApiClient
import ru.fromchat.api.AttachmentDownloadNotifier
import ru.fromchat.api.AttachmentDownloadProgress
import ru.fromchat.api.DmEnvelope
import ru.fromchat.api.DmFile
import ru.fromchat.crypto.decryptFile

object DmFileDownloader {
    suspend fun downloadToExportUri(
        messageId: Int,
        fileIndex: Int,
        file: DmFile,
        envelope: DmEnvelope,
        currentUserId: Int?,
        clientMessageId: String?,
        exportUri: String,
        messageLabel: String? = null,
    ): Boolean {
        val key = DownloadedFileRegistry.storageKey(messageId, fileIndex, clientMessageId)
        val label = AttachmentMediaLog.messageLabel(messageLabel)
        return withContext(Dispatchers.Default + NonCancellable) {
            runCatching {
                val written = AttachmentDownloadScheduler.run(storageKey = key, messageId = messageId) {
                    AttachmentDownloadNotifier.emit(
                        AttachmentDownloadProgress.InProgress(key, 1),
                        messageLabel = label,
                        messageId = messageId,
                        fileIndex = fileIndex,
                        clientMessageId = clientMessageId,
                        mirrorAsFileAttachment = true,
                    )
                    val bytes = decryptFile(
                        file = file,
                        envelope = envelope,
                        currentUserId = currentUserId,
                        downloadResumeKey = key,
                        onDownloadProgress = { percent ->
                            AttachmentDownloadNotifier.emit(
                                AttachmentDownloadProgress.InProgress(key, percent.coerceIn(0, 100)),
                                messageLabel = label,
                                messageId = messageId,
                                fileIndex = fileIndex,
                                clientMessageId = clientMessageId,
                                mirrorAsFileAttachment = true,
                            )
                        },
                    )
                    AttachmentDownloadNotifier.emit(
                        AttachmentDownloadProgress.InProgress(key, 99),
                        messageLabel = label,
                        messageId = messageId,
                        fileIndex = fileIndex,
                        clientMessageId = clientMessageId,
                        mirrorAsFileAttachment = true,
                    )
                    if (!writeBytesToExportUri(exportUri, bytes)) {
                        ApiClient.clearPartialEncryptedDownload(key)
                        AttachmentDownloadNotifier.emit(
                            AttachmentDownloadProgress.Failed(key, "write_failed"),
                            messageLabel = label,
                            messageId = messageId,
                            fileIndex = fileIndex,
                            clientMessageId = clientMessageId,
                            mirrorAsFileAttachment = true,
                        )
                        return@run null
                    }
                    DownloadedFileRegistry.setExportUri(
                        messageId = messageId,
                        fileIndex = fileIndex,
                        clientMessageId = clientMessageId,
                        exportUri = exportUri,
                    )
                    AttachmentDownloadNotifier.emit(
                        AttachmentDownloadProgress.Success(storageKey = key, messageId = messageId),
                        messageLabel = label,
                        messageId = messageId,
                        fileIndex = fileIndex,
                        clientMessageId = clientMessageId,
                        mirrorAsFileAttachment = true,
                    )
                    exportUri
                }
                written != null
            }.onFailure { error ->
                ApiClient.clearPartialEncryptedDownload(key)
                AttachmentDownloadNotifier.emit(
                    AttachmentDownloadProgress.Failed(
                        storageKey = key,
                        error = error.message ?: "download_failed",
                    ),
                    messageLabel = label,
                    messageId = messageId,
                    fileIndex = fileIndex,
                    clientMessageId = clientMessageId,
                    mirrorAsFileAttachment = true,
                )
            }.getOrDefault(false)
        }
    }
}

expect suspend fun writeBytesToExportUri(exportUri: String, bytes: ByteArray): Boolean

expect suspend fun isExportUriAccessible(exportUri: String): Boolean

expect fun openExportUri(exportUri: String, mimeType: String): Boolean
