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
import com.google.android.material.snackbar.Snackbar


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
// Register ActivityResultLauncher for both creating and editing notes
        createNoteLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val id = result.data?.getLongExtra("note_id", -1) ?: -1L
                val title = result.data?.getStringExtra("note_title") ?: ""
                val content = result.data?.getStringExtra("note_content") ?: ""

                if (id == -1L) {
                    // This is a new note, insert it into the database
                    val newNoteId = databaseHelper.insertNote(title, content)
                    val newNote = Note(newNoteId, title, content, NoteDatabaseHelper.getCurrentTimestamp())  // Use formatted timestamp
                    notesList.add(newNote)
                    notesAdapter.notifyItemInserted(notesList.size - 1)
                } else {
                    // This is an existing note, update it in the database
                    databaseHelper.updateNote(id, title, content)
                    val note = notesList.find { it.id == id }
                    note?.title = title
                    note?.content = content
                    note?.timestamp = NoteDatabaseHelper.getCurrentTimestamp()  // Set formatted timestamp
                    notesAdapter.notifyItemChanged(notesList.indexOf(note))
                }
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
        val newNotes = databaseHelper.getAllNotes()

        if (notesList.isEmpty()) {
            // If the list is initially empty, add all notes
            notesList.addAll(newNotes)
            notesAdapter.notifyItemRangeInserted(0, newNotes.size)
        } else {
            // If the list already has items, compare and update
            for (i in newNotes.indices) {
                if (i < notesList.size) {
                    // Update existing notes
                    if (notesList[i] != newNotes[i]) {
                        notesList[i] = newNotes[i]
                        notesAdapter.notifyItemChanged(i)
                    }
                } else {
                    // Insert new notes
                    notesList.add(newNotes[i])
                    notesAdapter.notifyItemInserted(i)
                }
            }
        }
    }

    override fun onNoteClicked(note: Note) {
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
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val noteToDelete = notesList[position]

                // Remove the note from the list temporarily
                notesList.removeAt(position)
                notesAdapter.notifyItemRemoved(position)

                // Show a Snackbar with an undo option
                val Snackbar = Snackbar.make(recyclerViewNotes, "Note deleted", Snackbar.LENGTH_LONG)
                Snackbar.setAction("Undo") {
                    // Reinsert the note if undo is pressed
                    notesList.add(position, noteToDelete)
                    notesAdapter.notifyItemInserted(position)
                }
                Snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event != DISMISS_EVENT_ACTION) {
                            // If the Snackbar is dismissed (not by clicking undo), delete from the database
                            databaseHelper.deleteNote(noteToDelete.id)
                        }
                    }
                })
                Snackbar.show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewNotes)
    }
}
