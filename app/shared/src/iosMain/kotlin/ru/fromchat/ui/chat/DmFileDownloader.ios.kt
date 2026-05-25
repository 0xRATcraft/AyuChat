@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ru.fromchat.ui.chat

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import ru.fromchat.platform.iosTopViewController

actual suspend fun writeBytesToExportUri(exportUri: String, bytes: ByteArray): Boolean =
    withContext(Dispatchers.Default) {
        val url = NSURL.URLWithString(exportUri) ?: NSURL.fileURLWithPath(exportUri.removePrefix("file://"))
        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        } ?: return@withContext false
        nsData.writeToURL(url, true) || run {
            val path = url.path ?: return@withContext false
            nsData.writeToFile(path, true)
        }
    }

actual suspend fun isExportUriAccessible(exportUri: String): Boolean = withContext(Dispatchers.Default) {
    val url = NSURL.URLWithString(exportUri) ?: NSURL.fileURLWithPath(exportUri.removePrefix("file://"))
    val path = url.path
    if (path != null) {
        return@withContext NSFileManager.defaultManager.fileExistsAtPath(path)
    }
    runCatching {
        NSFileManager.defaultManager.isReadableFileAtPath(url.absoluteString ?: return@runCatching false)
    }.getOrDefault(false)
}

actual fun openExportUri(exportUri: String, mimeType: String): Boolean {
    val url = NSURL.URLWithString(exportUri) ?: NSURL.fileURLWithPath(exportUri.removePrefix("file://"))
    val host = iosTopViewController() ?: return false
    return UIApplication.sharedApplication.openURL(url)
}
