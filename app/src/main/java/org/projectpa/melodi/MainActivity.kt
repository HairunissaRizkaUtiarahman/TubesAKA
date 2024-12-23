package org.projectpa.melodi

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.projectpa.melodi.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val noteToFileMap = mapOf(
        "C" to "c",
        "C#" to "note_c_sharp",
        "D" to "d",
        "D#" to "d_sharp",
        "E" to "e",
        "F" to "f",
        "F#" to "f_sharp",
        "G" to "g",
        "G#" to "g_sharp",
        "A" to "note_a",
        "A#" to "a_sharp",
        "B" to "b"
    )

    private val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private lateinit var binding: ActivityMainBinding
    private var generatedMelody: List<String> = listOf()
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, notes)
        binding.dropdownInitialNote.adapter = adapter

        binding.generateButton.setOnClickListener { generateMelody() }
        binding.playMelodyButton.setOnClickListener { playMelody() }
        binding.stopMelodyButton.setOnClickListener { stopMelody() }
    }

    private fun generateMelody() {
        val startNote = binding.dropdownInitialNote.selectedItem.toString()
        val length = binding.inputMelodyLength.text.toString().toIntOrNull()
        val interval = binding.inputInterval.text.toString().toIntOrNull()

        if (length == null || interval == null || length <= 0 || interval <= 0) {
            Toast.makeText(this, "Please provide valid numeric inputs!", Toast.LENGTH_SHORT).show()
            return
        }

        var recursiveMelody: List<String> = listOf()
        val recursiveStart = System.nanoTime()
        recursiveMelody = generateRecursiveMelody(startNote, length, interval)
        val recursiveTime = (System.nanoTime() - recursiveStart) / 1_000_000.0 // Convert to ms

        var iterativeMelody: List<String> = listOf()
        val iterativeStart = System.nanoTime()
        iterativeMelody = generateIterativeMelody(startNote, length, interval)
        val iterativeTime = (System.nanoTime() - iterativeStart) / 1_000_000.0 // Convert to ms

        binding.outputRecursive.text = recursiveMelody.joinToString(", ")
        binding.outputIterative.text = iterativeMelody.joinToString(", ")
        binding.timeRecursive.text = "%.2f ms".format(recursiveTime)
        binding.timeIterative.text = "%.2f ms".format(iterativeTime)

        generatedMelody = recursiveMelody
    }

    private fun generateRecursiveMelody(
        startNote: String,
        length: Int,
        interval: Int,
        melody: MutableList<String> = mutableListOf()
    ): List<String> {
        if (length == 0) return melody
        melody.add(startNote)
        val nextIndex = (notes.indexOf(startNote) + interval) % notes.size
        return generateRecursiveMelody(notes[nextIndex], length - 1, interval, melody)
    }

    private fun generateIterativeMelody(startNote: String, length: Int, interval: Int): List<String> {
        val melody = mutableListOf<String>()
        var currentNote = startNote
        for (i in 0 until length) {
            melody.add(currentNote)
            val nextIndex = (notes.indexOf(currentNote) + interval) % notes.size
            currentNote = notes[nextIndex]
        }
        return melody
    }

    private fun playMelody() {
        if (generatedMelody.isEmpty()) {
            Toast.makeText(this, "No melody to play! Generate one first.", Toast.LENGTH_SHORT).show()
            return
        }

        stopMelody()

        Thread {
            isPlaying = true
            for (note in generatedMelody) {
                if (!isPlaying) break

                val fileName = noteToFileMap[note] ?: continue
                val resId = resources.getIdentifier(fileName, "raw", packageName)
                if (resId == 0) {
                    runOnUiThread {
                        Toast.makeText(this, "File audio untuk $note tidak ditemukan!", Toast.LENGTH_SHORT).show()
                    }
                    continue
                }

                mediaPlayer = MediaPlayer.create(this, resId)
                mediaPlayer?.start()

                while (mediaPlayer?.isPlaying == true) {
                    Thread.sleep(100)
                }

                mediaPlayer?.release()
                mediaPlayer = null
            }

            isPlaying = false
        }.start()
    }

    private fun stopMelody() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isPlaying = false
    }
}