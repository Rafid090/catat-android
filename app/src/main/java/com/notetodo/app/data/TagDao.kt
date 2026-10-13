package com.notetodo.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    /** Reuses an existing tag rather than creating a near-duplicate. */
    @Transaction
    suspend fun getOrCreate(name: String): Long {
        val trimmed = name.trim()
        findByName(trimmed)?.let { return it.id }
        val id = insert(TagEntity(name = trimmed))
        return if (id != -1L) id else findByName(trimmed)?.id ?: -1L
    }

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun delete(tagId: Long)

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    suspend fun tagIdsForNote(noteId: Long): List<Long>

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearNoteTags(noteId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkNote(ref: NoteTagCrossRef)

    @Transaction
    suspend fun setNoteTags(noteId: Long, tagIds: List<Long>) {
        clearNoteTags(noteId)
        tagIds.forEach { linkNote(NoteTagCrossRef(noteId, it)) }
    }

    @Query("SELECT tagId FROM task_tags WHERE taskId = :taskId")
    suspend fun tagIdsForTask(taskId: Long): List<Long>

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun clearTaskTags(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTask(ref: TaskTagCrossRef)

    @Transaction
    suspend fun setTaskTags(taskId: Long, tagIds: List<Long>) {
        clearTaskTags(taskId)
        tagIds.forEach { linkTask(TaskTagCrossRef(taskId, it)) }
    }
}
