package ru.fromchat.api.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fromchat.api.DmConversation
import ru.fromchat.api.Message
import ru.fromchat.db.Conversation
import ru.fromchat.db.MessageDatabase
import ru.fromchat.db.Message as DbMessage

/**
 * Simple repository wrapping [MessageDatabase] for caching messages and conversations.
 *
 * This is intentionally minimal – it focuses on the flows the app needs for
 * public chat and DMs rather than trying to mirror the entire backend schema.
 */
data class CachedConversation(
    val id: String,
    val otherUserId: Int,
    val displayName: String,
    val lastMessagePreview: String?,
    val unreadCount: Int
)

object MessageCacheStore {
    private val db: MessageDatabase get() = MessageDatabaseProvider.database

    private fun conversationIdForPublic(): String = "public"
    private fun conversationIdForDm(otherUserId: Int): String = "dm:$otherUserId"

    suspend fun loadPublicMessages(): List<Message> {
        return loadMessages(conversationIdForPublic())
    }

    suspend fun replacePublicMessages(messages: List<Message>) {
        replaceMessages(conversationIdForPublic(), messages)
    }

    suspend fun loadDmMessages(otherUserId: Int): List<Message> {
        return loadMessages(conversationIdForDm(otherUserId))
    }

    suspend fun replaceDmMessages(otherUserId: Int, messages: List<Message>) {
        replaceMessages(conversationIdForDm(otherUserId), messages)
    }

    private suspend fun loadMessages(conversationId: String): List<Message> =
        withContext(Dispatchers.Default) {
            db.messageDatabaseQueries
                .selectMessagesByConversation(conversationId)
                .executeAsList()
                .map { row: DbMessage ->
                    Message(
                        id = row.id.toInt(),
                        user_id = row.userId.toInt(),
                        content = row.content,
                        timestamp = row.timestamp,
                        is_read = row.isRead != 0L,
                        is_edited = row.isEdited != 0L,
                        username = "", // Filled from network; cache focuses on content & ordering.
                        profile_picture = null,
                        verified = null,
                        reply_to = null,
                        client_message_id = row.clientMessageId,
                        reactions = null,
                        files = null
                    )
                }
        }

    private suspend fun replaceMessages(conversationId: String, messages: List<Message>) {
        withContext(Dispatchers.Default) {
            db.messageDatabaseQueries.transaction {
                db.messageDatabaseQueries.deleteMessagesForConversation(conversationId)
                messages.forEach { msg ->
                    db.messageDatabaseQueries.upsertMessage(
                        id = msg.id.toLong(),
                        conversationId = conversationId,
                        userId = msg.user_id.toLong(),
                        content = msg.content,
                        timestamp = msg.timestamp,
                        isRead = if (msg.is_read) 1L else 0L,
                        isEdited = if (msg.is_edited) 1L else 0L,
                        replyToId = msg.reply_to?.id?.toLong(),
                        clientMessageId = msg.client_message_id,
                        deletedFlag = 0L
                    )
                }
            }
        }
    }

    suspend fun markMessageDeleted(conversationId: String, messageId: Int) {
        withContext(Dispatchers.Default) {
            db.messageDatabaseQueries.markMessageDeleted(
                id = messageId.toLong(),
                conversationId = conversationId
            )
        }
    }

    suspend fun replaceDmConversations(conversations: List<DmConversation>) {
        withContext(Dispatchers.Default) {
            db.messageDatabaseQueries.transaction {
                conversations.forEach { conv ->
                    val conversationId = conversationIdForDm(conv.user.id)
                    db.messageDatabaseQueries.upsertConversation(
                        id = conversationId,
                        type = "dm",
                        otherUserId = conv.user.id.toLong(),
                        displayName = conv.user.username,
                        lastMessageId = conv.lastMessage.id.toLong(),
                        lastMessagePreview = null, // Encrypted on backend; preview handled in chat UI.
                        unreadCount = conv.unreadCount.toLong(),
                        updatedAt = conv.lastMessage.timestamp
                    )
                }
            }
        }
    }

    suspend fun loadCachedDmConversations(): List<CachedConversation> =
        withContext(Dispatchers.Default) {
            db.messageDatabaseQueries
                .selectConversations()
                .executeAsList()
                .filter { row: Conversation -> row.type == "dm" }
                .map { row: Conversation ->
                    CachedConversation(
                        id = row.id,
                        otherUserId = row.otherUserId?.toInt() ?: 0,
                        displayName = row.displayName ?: "",
                        lastMessagePreview = row.lastMessagePreview,
                        unreadCount = row.unreadCount.toInt()
                    )
                }
        }
}

