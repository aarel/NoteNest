package com.dev.notenest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(
    private val notes: List<Note>,
    private val listener: NoteItemListener
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    interface NoteItemListener {
        fun onNoteClicked(note: Note)
        fun onDeleteSelectedNotes(selectedNotes: List<Note>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.titleTextView.text = note.title

        // Use the string resource with placeholders for the timestamp
        holder.timestampTextView.text = holder.itemView.context.getString(
            R.string.note_timestamp,
            note.timestamp
        )

        holder.itemView.setOnClickListener {
            listener.onNoteClicked(note) // Call listener when a note is clicked
        }
    }

    override fun getItemCount(): Int = notes.size

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val timestampTextView: TextView = itemView.findViewById(R.id.textViewTimestamp)
    }
}