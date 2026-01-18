package com.example.wind

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wind.databinding.ActivityMainBinding
import com.example.wind.databinding.DialogAddEventBinding
import com.example.wind.mqtt.MqttManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mqttManager: MqttManager
    private lateinit var adapter: EventAdapter

    private var allEvents: List<EventItem> = emptyList()

    private val REQUEST_CAMERA = 1
    private val TAKE_PHOTO = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupMqtt()
        setupButtons()

        loadEvents()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun setupMqtt() {
        mqttManager = MqttManager(
            context = this,
            serverUri = "tcp://192.168.1.10:1883",
            resultTopic = "windsync/result",
            imageTopic = "windsync/image",
            onResult = { handleResult(it) },
            onStatus = { } // ne uporabljamo več text statusa
        )

        mqttManager.connect()
    }

    private fun handleResult(payload: String) {
        try {
            val json = JSONObject(payload)

            val isWind = json.getBoolean("is_wind_turbine")
            val confidence = json.getDouble("confidence")

            runOnUiThread {
                if (isWind) {
                    val blades = json.getString("blades")
                    val bladesConf = json.getDouble("blades_confidence")

                    showResultCard(true, confidence, blades, bladesConf)
                } else {
                    showResultCard(false, confidence)
                }
            }

        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Napaka pri rezultatu", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showResultCard(
        isWind: Boolean,
        confidence: Double,
        blades: String? = null,
        bladesConf: Double? = null
    ) {
        val card = binding.resultCardView

        val title = card.txtTitle
        val txtBlades = card.txtBlades
        val txtConfidence = card.txtConfidence

        if (isWind) {
            title.text = "JE vetrnica"
            txtBlades.text = "Krakov: $blades"
        } else {
            title.text = "NI vetrnica"
            txtBlades.text = ""
        }

        txtConfidence.text = "Zanesljivost: ${(confidence * 100).toInt()} %"

        card.root.visibility = android.view.View.VISIBLE
        card.root.alpha = 0f

        card.root.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        card.root.postDelayed({
            card.root.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    card.root.visibility = android.view.View.GONE
                }
                .start()
        }, 3000)
    }


    private fun setupRecycler() {

        adapter = EventAdapter(mutableListOf()) { event ->
            showEditEventDialog(event)
        }

        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = adapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val event = allEvents[position]
                showDeleteConfirmDialog(event, position)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvEvents)
    }


    private fun loadEvents() {
        lifecycleScope.launch {
            val res = ApiService.getEvents(null)

            if (res.isFailure) {
                Toast.makeText(
                    this@MainActivity,
                    "Napaka: ${res.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            allEvents = parseEvents(res.getOrNull().orEmpty())
            adapter.updateData(allEvents)
        }
    }

    private fun parseEvents(json: String): List<EventItem> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<EventItem>()

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)

                val loc = o.optJSONObject("location")
                val coords = loc?.optJSONArray("coordinates")

                list.add(
                    EventItem(
                        o.optString("_id"),
                        o.optString("topic"),
                        o.optString("message"),
                        o.optString("timestamp"),
                        coords?.optDouble(1) ?: 0.0,
                        coords?.optDouble(0) ?: 0.0
                    )
                )
            }
            list

        } catch (e: Exception) {
            emptyList()
        }
    }


    private fun showDeleteConfirmDialog(event: EventItem, position: Int) {

        AlertDialog.Builder(this)
            .setTitle("Izbriši dogodek")
            .setMessage(event.message)
            .setPositiveButton("Izbriši") { _, _ ->

                lifecycleScope.launch {
                    val res = ApiService.deleteEvent(event.id)

                    if (res.isSuccess) {
                        loadEvents()
                    } else {
                        adapter.notifyItemChanged(position)
                        Toast.makeText(this@MainActivity,
                            "Napaka pri brisanju", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Prekliči") { _, _ ->
                adapter.notifyItemChanged(position)
            }
            .show()
    }


    private fun showEditEventDialog(event: EventItem) {

        val topics = arrayOf(
            "veter/prevec",
            "veter/premalo",
            "veternica/okvara",
            "veternica/servis"
        )

        val dialogBinding = DialogAddEventBinding.inflate(layoutInflater)

        dialogBinding.etMessage.setText(event.message)

        dialogBinding.spinnerTopic.adapter =
            ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, topics)

        val index = topics.indexOf(event.topic)
        if (index >= 0) dialogBinding.spinnerTopic.setSelection(index)

        AlertDialog.Builder(this)
            .setTitle("Uredi dogodek")
            .setView(dialogBinding.root)
            .setPositiveButton("Shrani") { _, _ ->

                lifecycleScope.launch {
                    ApiService.postEvent(
                        dialogBinding.spinnerTopic.selectedItem.toString(),
                        dialogBinding.etMessage.text.toString(),
                        event.lat,
                        event.lon
                    )
                    loadEvents()
                }
            }
            .setNegativeButton("Prekliči", null)
            .show()
    }

    private fun setupButtons() {

        val nav = binding.bottomNav

        nav.btnNavCamera.setOnClickListener { checkCameraPermissionAndOpen() }

        nav.btnNavAdd.setOnClickListener {
            startActivity(Intent(this, MessageActivity::class.java))
        }

        nav.btnNavMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.btnGenerateEvent.setOnClickListener {
            startActivity(Intent(this, SimulationActivity::class.java))
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA)

        } else openCamera()
    }

    private fun openCamera() {
        startActivityForResult(
            Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),
            TAKE_PHOTO
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as Bitmap
            sendBitmap(bitmap)
        }
    }

    private fun sendBitmap(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)

        mqttManager.publishImage(
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        )
    }

    override fun onDestroy() {
        mqttManager.disconnect()
        super.onDestroy()
    }
}
