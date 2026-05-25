package ru.fromchat.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fromchat.core.cache.CacheContext
import ru.fromchat.core.cache.stageOutboundFileForUpload

/**
 * Copy a non-image attachment into instance upload storage (same pipeline as images).
 */
suspend fun prepareOutboundFileForSend(
    clientMessageId: String,
    sourceUri: String,
): StagedOutboundPreview? = withContext(Dispatchers.Default) {
    val instanceId = runCatching { CacheContext.requireActiveInstanceId() }.getOrNull() ?: return@withContext null
    val staged = runCatching {
        stageOutboundFileForUpload(instanceId, clientMessageId, sourceUri)
    }.getOrNull() ?: return@withContext null
    if (staged.sizeBytes <= 0L) return@withContext null
    StagedOutboundPreview(stagedUri = staged.uri, aspectRatio = null)
}
