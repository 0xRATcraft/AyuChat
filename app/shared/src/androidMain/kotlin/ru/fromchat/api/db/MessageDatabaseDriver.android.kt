package ru.fromchat.api.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.pr0gramm3r101.utils.UtilsLibrary
import ru.fromchat.db.MessageDatabase

actual fun provideMessageDatabaseDriver(): SqlDriver {
    return AndroidSqliteDriver(
        schema = MessageDatabase.Schema,
        context = UtilsLibrary.context,
        name = "message_database.db"
    )
}

