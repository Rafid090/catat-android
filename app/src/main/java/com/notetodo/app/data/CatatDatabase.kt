package com.notetodo.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        TaskTagCrossRef::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class CatatDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao
}

/** Adds TaskEntity.reminderAt. Migrating keeps notes and tasks already on the device. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderAt INTEGER")
    }
}

/**
 * Adds tags and the two link tables. The statements are copied verbatim from the
 * schema Room generates for version 3; Room compares them column for column and
 * rejects anything that differs.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `note_tags` " +
                "(`noteId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`noteId`, `tagId`), " +
                "FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tags_tagId` ON `note_tags` (`tagId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `task_tags` " +
                "(`taskId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`taskId`, `tagId`), " +
                "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_tags_tagId` ON `task_tags` (`tagId`)")
    }
}
