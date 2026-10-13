package com.notetodo.app.data

import android.content.Context
import androidx.room.Room
import com.notetodo.app.reminder.ReminderScheduler

/**
 * Hand-rolled dependency container. Small enough that Hilt would be overkill;
 * swap this out if the graph grows.
 */
class AppContainer(context: Context) {

    private val database: CatatDatabase = Room
        .databaseBuilder(context.applicationContext, CatatDatabase::class.java, "catat.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    val noteDao: NoteDao get() = database.noteDao()
    val taskDao: TaskDao get() = database.taskDao()
    val tagDao: TagDao get() = database.tagDao()

    val reminderScheduler = ReminderScheduler(context.applicationContext)
}
