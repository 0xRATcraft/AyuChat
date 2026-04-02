package ru.fromchat.api.db

import app.cash.sqldelight.db.SqlDriver
import ru.fromchat.db.MessageDatabase

/**
 * Platform-agnostic access to the SQLDelight [MessageDatabase].
 */
expect fun provideMessageDatabaseDriver(): SqlDriver

object MessageDatabaseProvider {
    val database: MessageDatabase by lazy {
        MessageDatabase(provideMessageDatabaseDriver())
    }
}

