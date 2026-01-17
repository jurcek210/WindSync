package com.example.wind

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wind.databinding.ActivityEventsListBinding
import com.example.wind.databinding.DialogAddEventBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class EventsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventsListBinding
    private lateinit var adapter: EventAdapter

    private var allEvents: List<EventItem> = emptyList()

    private enum class Filter { ALL, VETER, VETERNICE }
    private var filter = Filter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEventsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        class EventAdapter(
            private val events: MutableList<EventItem>
        )

        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = adapter

        setupFilterSpinner()
        loadEvents()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun setupFilterSpinner() {
        val items = listOf("All", "Veter", "Vetrnice")
        binding.spinnerFilter.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)

        binding.spinnerFilter.setOnItemSelectedListener { _, _, pos, _ ->
            filter = when (pos) {
                1 -> Filter.VETER
                2 -> Filter.VETERNICE
                else -> Filter.ALL
            }
            applyFilter()
        }
    }


    private fun android.widget.Spinner.setOnItemSelectedListener(
        onSelected: (parent: android.widget.AdapterView<*>, view: android.view.View?, pos: Int, id: Long) -> Unit
    ) {
        onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, pos: Int, id: Long) =
                onSelected(parent, view, pos, id)
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun loadEvents() {
        lifecycleScope.launch {
            val res = ApiService.getEvents(null)
            if (res.isFailure) {
                Toast.makeText(this@EventsListActivity,
                    "Napaka: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                return@launch
            }

            val json = res.getOrNull().orEmpty()
            allEvents = parseEvents(json)
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = when (filter) {
            Filter.ALL -> allEvents
            Filter.VETER -> allEvents.filter { it.topic.startsWith("veter/") }
            Filter.VETERNICE -> allEvents.filter { it.topic.startsWith("veternica/") }
        }
        adapter.updateData(filtered)
    }


    private fun parseEvents(json: String): List<EventItem> {
        return try {
            val arr = JSONArray(json)
            val out = mutableListOf<EventItem>()

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)

                val id = o.optString("_id", "")
                val topic = o.optString("topic", "")
                val message = o.optString("message", "")
                val timestamp = o.optString("timestamp", "")

                val loc = o.optJSONObject("location") ?: JSONObject()
                val coords = loc.optJSONArray("coordinates")

                val lon = coords?.optDouble(0) ?: 0.0
                val lat = coords?.optDouble(1) ?: 0.0

                out.add(EventItem(id, topic, message, timestamp, lat, lon))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }
}
