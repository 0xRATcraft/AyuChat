package ru.fromchat.api.local.messages

import kotlinx.coroutines.sync.Mutex
import ru.fromchat.api.schema.messages.Message

private val optimisticIdMutex = Mutex()
private var nextOptimisticSeq = 0
private val idByClientMessageId = mutableMapOf<String, Int>()

private inline fun <T> withOptimisticIdLock(block: () -> T): T {
    while (!optimisticIdMutex.tryLock()) {
        // spin — allocation is rare and short
    }
    try {
        return block()
    } finally {
        optimisticIdMutex.unlock()
    }
}

/**
 * Negative row id for an optimistic / outbox [clientMessageId].
 * Monotonic enqueue order (-1, -2, …); stable for the same client id within process.
 */
fun optimisticMessageIdForClientMessageId(clientMessageId: String): Int {
    val key = clientMessageId.trim()
    if (key.isEmpty()) return nextOptimisticMessageId()
    return withOptimisticIdLock {
        idByClientMessageId.getOrPut(key) {
            nextOptimisticSeq += 1
            -nextOptimisticSeq
        }
    }
}

/** Fresh monotonic negative id when no client message id is available yet. */
fun nextOptimisticMessageId(): Int = withOptimisticIdLock {
    nextOptimisticSeq += 1
    -nextOptimisticSeq
}

fun Message.isQueuedOutbound(): Boolean = id < 0
