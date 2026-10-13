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
interface NoteDao {

    /** [tagId] of 0 means "any tag"; real ids start at 1. */
    @Transaction
    @Query(
        """
        SELECT * FROM notes
        WHERE (:query = ''
               OR title LIKE '%' || :query || '%'
               OR content LIKE '%' || :query || '%')
          AND (:tagId = 0
               OR EXISTS (SELECT 1 FROM note_tags
                          WHERE note_tags.noteId = notes.id AND note_tags.tagId = :tagId))
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun observeNotes(query: String, tagId: Long): Flow<List<NoteWithTags>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    /** REPLACE lets undo re-insert a deleted note under its original id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}
