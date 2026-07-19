package ru.fromchat.api.schema.websocket.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AckUpdatesRequest(
    @SerialName("lastSeq") val lastSeq: Int,
)
