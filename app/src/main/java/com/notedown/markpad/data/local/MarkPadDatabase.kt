package com.notedown.markpad.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notedown.markpad.data.SeedNotes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class MarkPadDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: MarkPadDatabase? = null

        fun getInstance(context: Context): MarkPadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): MarkPadDatabase {
            return Room.databaseBuilder(
                context,
                MarkPadDatabase::class.java,
                "markpad_database.db"
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        getInstance(context).noteDao().insertAll(
                            SeedNotes.items.map { NoteEntity.fromDomain(it) }
                        )
                    }
                }
            }).build()
        }
    }
}
