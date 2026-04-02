package ru.fromchat.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebSocketUpdatesData(
    val seq: Int,
    val updates: List<WebSocketMessage>
)

@Serializable
data class GetUpdatesRequest(
    @SerialName("lastSeq") val lastSeq: Int
)

@Serializable
data class GetUpdatesResponse(
    val status: String,
    @SerialName("lastSeq") val lastSeq: Int,
    @SerialName("missedCount") val missedCount: Int
)

@Serializable
data class ReactionUpdateData(
    val message_id: Int,
    val emoji: String,
    val action: String,
    val user_id: Int,
    val username: String,
    val reactions: List<ReactionData>
)

@Serializable
data class ReactionData(
    val emoji: String,
    val count: Int,
    val users: List<ReactionUser>
)

@Serializable
data class ReactionUser(
    val id: Int,
    val username: String
)

@Serializable
data class TypingUpdateData(
    val userId: Int,
    val username: String
)