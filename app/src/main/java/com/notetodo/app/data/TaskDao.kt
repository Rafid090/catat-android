package com.notetodo.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /**
     * Unfinished first, then by due date (undated last), then priority, then newest.
     * [doneFilter]: -1 = all, 0 = active only, 1 = done only.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM tasks
        WHERE (:doneFilter = -1 OR isDone = :doneFilter)
          AND (:query = ''
               OR title LIKE '%' || :query || '%'
               OR note LIKE '%' || :query || '%')
          AND (:tagId = 0
               OR EXISTS (SELECT 1 FROM task_tags
                          WHERE task_tags.taskId = tasks.id AND task_tags.tagId = :tagId))
        ORDER BY isDone ASC,
                 CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END ASC,
                 dueAt ASC,
                 isHighPriority DESC,
                 createdAt DESC
        """
    )
    fun observeTasks(query: String, doneFilter: Int, tagId: Long): Flow<List<TaskWithTags>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    /** REPLACE lets undo re-insert a deleted task under its original id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun clearCompleted()

    /** Alarms do not survive a reboot, so they are rebuilt from this on boot. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isDone = 0 AND reminderAt IS NOT NULL AND reminderAt > :now
        """
    )
    suspend fun getPendingReminders(now: Long): List<TaskEntity>
}
