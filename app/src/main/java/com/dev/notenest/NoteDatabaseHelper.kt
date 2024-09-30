package com.dev.notenest

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class NoteDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "notes.db"
        const val DATABASE_VERSION = 2
        const val TABLE_NAME = "notes"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_TIMESTAMP = "timestamp"

        // Function to get the current timestamp in MM/dd/YYYY HH:mm format
        fun getCurrentTimestamp(): String {
            val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_TIMESTAMP TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TIMESTAMP TEXT DEFAULT CURRENT_TIMESTAMP")
        }
    }

    // Insert a new note with a formatted timestamp
    fun insertNote(title: String, content: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_TIMESTAMP, getCurrentTimestamp())  // Use formatted timestamp
        }

        return try {
            db.insert(TABLE_NAME, null, values)
        } catch (e: SQLException) {
            -1L // Return -1 if insertion fails
        }
    }

    // Update an existing note with a new timestamp
    fun updateNote(id: Long, title: String, content: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_TIMESTAMP, getCurrentTimestamp())  // Update with the new timestamp
        }

        return try {
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        } catch (e: SQLException) {
            0 // Return 0 if update fails
        }
    }

    // Get all notes (Optimize by selecting only the needed columns)
    fun getAllNotes(): List<Note> {
        val db = readableDatabase
        val notes = mutableListOf<Note>()
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT, COLUMN_TIMESTAMP), // Specify columns to retrieve
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
            val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            notes.add(Note(id, title, content, timestamp))
        }
        cursor.close()
        return notes
    }

    // Delete a note with error handling
    fun deleteNote(id: Long): Int {
        val db = writableDatabase
        return try {
            db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        } catch (e: SQLException) {
            0 // Return 0 if deletion fails
        }
    }
}
