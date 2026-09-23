package com.example.notes

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

data class Note(val text: String, var done: Boolean)

class MainActivity : AppCompatActivity() {

    private val notes = mutableListOf<Note>()
    private lateinit var adapter: NotesAdapter
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.noteInput)
        val addBtn = findViewById<Button>(R.id.addBtn)
        val recycler = findViewById<RecyclerView>(R.id.recycler)

        loadNotes()

        adapter = NotesAdapter(notes) { position ->
            notes[position].done = !notes[position].done
            saveNotes()
            adapter.notifyItemChanged(position)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        addBtn.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                notes.add(0, Note(text, false))
                input.text.clear()
                saveNotes()
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun saveNotes() {
        val arr = JSONArray()
        notes.forEach {
            val obj = JSONObject()
            obj.put("text", it.text)
            obj.put("done", it.done)
            arr.put(obj)
        }
        getSharedPreferences("notes", Context.MODE_PRIVATE)
            .edit().putString("data", arr.toString()).apply()
    }

    private fun loadNotes() {
        val s = getSharedPreferences("notes", Context.MODE_PRIVATE)
            .getString("data", "[]") ?: "[]"
        val arr = JSONArray(s)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            notes.add(Note(obj.getString("text"), obj.getBoolean("done")))
        }
    }
}

class NotesAdapter(
    private val items: MutableList<Note>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<NotesAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.noteText)
        val del: ImageButton = v.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val note = items[position]
        holder.text.text = note.text
        if (note.done) {
            holder.text.paintFlags = holder.text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.text.alpha = 0.5f
        } else {
            holder.text.paintFlags = holder.text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.text.alpha = 1f
        }
        holder.itemView.setOnClickListener { onClick(holder.adapterPosition) }
        holder.del.setOnClickListener {
            val pos = holder.adapterPosition
            items.removeAt(pos)
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, items.size)
            val ctx = holder.itemView.context
            val prefs = ctx.getSharedPreferences("notes", Context.MODE_PRIVATE)
            val arr = JSONArray()
            items.forEach { n ->
                val o = JSONObject()
                o.put("text", n.text)
                o.put("done", n.done)
                arr.put(o)
            }
            prefs.edit().putString("data", arr.toString()).apply()
        }
    }

    override fun getItemCount() = items.size
}
