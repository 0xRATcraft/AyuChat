package ru.fromchat.api.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import ru.fromchat.db.MessageDatabase

actual fun provideMessageDatabaseDriver(): SqlDriver {
    return NativeSqliteDriver(
        schema = MessageDatabase.Schema,
        name = "message_database.db"
    )
}

