package ru.fromchat.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UserStatus(
    val online: Boolean,
    val lastSeen: String? = null
)

object UserStatusStore {
    private val _status = MutableStateFlow<Map<Int, UserStatus>>(emptyMap())
    val status: StateFlow<Map<Int, UserStatus>> = _status.asStateFlow()

    fun update(userId: Int, online: Boolean, lastSeen: String?) {
        _status.update { it + (userId to UserStatus(online, lastSeen)) }
    }
}
