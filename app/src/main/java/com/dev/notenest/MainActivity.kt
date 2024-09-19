package com.dev.notenest

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.ItemTouchHelper

class MainActivity : AppCompatActivity(), NotesAdapter.NoteItemListener {

    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var databaseHelper: NoteDatabaseHelper
    private val notesList = mutableListOf<Note>()

    // Register ActivityResultLauncher for CreateNoteActivity
    private lateinit var createNoteLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)
        fabAddNote = findViewById(R.id.fabAddNote)
        databaseHelper = NoteDatabaseHelper(this)

        // Set up RecyclerView
        recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        notesAdapter = NotesAdapter(notesList, this)
        recyclerViewNotes.adapter = notesAdapter

        // Register ActivityResultLauncher
        createNoteLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val title = result.data?.getStringExtra("note_title") ?: ""
                val content = result.data?.getStringExtra("note_content") ?: ""

                // Insert the new note into the database and list
                val newNoteId = databaseHelper.insertNote(title, content)
                val newNote = Note(newNoteId, title, content, System.currentTimeMillis().toString())
                notesList.add(newNote)
                notesAdapter.notifyItemInserted(notesList.size - 1)  // Notify adapter about the new note
            }
        }

        // FAB click listener to create a new note
        fabAddNote.setOnClickListener {
            val intent = Intent(this, CreateNoteActivity::class.java)
            createNoteLauncher.launch(intent)
        }

        // Load notes from the database on start
        loadNotesFromDatabase()

        // Enable swipe-to-delete functionality
        enableSwipeToDelete()
    }

    private fun loadNotesFromDatabase() {
        val currentListSize = notesList.size  // Get the current size of the list
        val newNotes = databaseHelper.getAllNotes()  // Fetch all notes from the database

        // Clear the old notes and add the new notes from the database
        notesList.clear()
        notesList.addAll(newNotes)

        // Notify the adapter that new items were inserted
        notesAdapter.notifyItemRangeInserted(0, notesList.size - currentListSize)
    }


    override fun onEditNoteClicked(note: Note) {
        // Create an Intent to launch CreateNoteActivity for editing the note
        val intent = Intent(this, CreateNoteActivity::class.java).apply {
            putExtra("note_id", note.id)
            putExtra("note_title", note.title)
            putExtra("note_content", note.content)
        }

        // Launch CreateNoteActivity with the note data to edit
        createNoteLauncher.launch(intent)
    }


    override fun onDeleteSelectedNotes(selectedNotes: List<Note>) {
        // Store positions of the notes to be deleted
        val positionsToDelete = selectedNotes.map { note ->
            notesList.indexOf(note)
        }

        // Remove each selected note from the list and database
        selectedNotes.forEach { note ->
            databaseHelper.deleteNote(note.id)
            notesList.remove(note)  // Remove the note from the list
        }

        // Notify the adapter in reverse order to avoid shifting issues
        positionsToDelete.sortedDescending().forEach { position ->
            notesAdapter.notifyItemRemoved(position)  // Efficiently notify adapter for each removed item
        }

        // Optionally, display a message to the user
        Toast.makeText(this, "${selectedNotes.size} notes deleted.", Toast.LENGTH_SHORT).show()
    }


    private fun enableSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false  // We are not handling move events
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Get the position of the swiped note using bindingAdapterPosition
                val position = viewHolder.bindingAdapterPosition  // Replaces deprecated adapterPosition
                val noteToDelete = notesList[position]

                // Remove the note from the database
                databaseHelper.deleteNote(noteToDelete.id)

                // Remove the note from the list and notify the adapter
                notesList.removeAt(position)
                notesAdapter.notifyItemRemoved(position)

                // Optionally, show a toast to confirm deletion
                Toast.makeText(this@MainActivity, "Note deleted", Toast.LENGTH_SHORT).show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewNotes)
    }

}
