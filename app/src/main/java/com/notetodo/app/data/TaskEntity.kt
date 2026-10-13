package com.notetodo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val note: String = "",
    val isDone: Boolean = false,
    val isHighPriority: Boolean = false,
    /** Start of the due day, epoch millis in the device time zone. Null = no due date. */
    val dueAt: Long? = null,
    /** Exact instant to fire the notification at. Null = no reminder. */
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
