package com.dev.notenest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class CreateNoteActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextContent: EditText
    private lateinit var buttonSave: Button
    private var noteId: Long = -1L  // Default to -1 (new note mode)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        // Initialize views
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        buttonSave = findViewById(R.id.buttonSave)

        // Check if we're editing an existing note
        if (intent.hasExtra("note_id")) {
            noteId = intent.getLongExtra("note_id", -1L)
            val noteTitle = intent.getStringExtra("note_title")
            val noteContent = intent.getStringExtra("note_content")

            // Set the title and content in the EditText fields
            editTextTitle.setText(noteTitle)
            editTextContent.setText(noteContent)
        }

        // Save button click listener
        buttonSave.setOnClickListener {
            val noteTitle = editTextTitle.text.toString()
            val noteContent = editTextContent.text.toString()

            val resultIntent = Intent().apply {
                putExtra("note_id", noteId)
                putExtra("note_title", noteTitle)
                putExtra("note_content", noteContent)
            }

            // Return result and close activity
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
