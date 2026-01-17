package com.example.wind

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wind.databinding.ActivityMainBinding
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

        val nav = binding.bottomNav

        adapter = EventAdapter(mutableListOf())
        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = adapter

        mqttManager = MqttManager(
            context = this,
            serverUri = "tcp://192.168.1.10:1883",
            resultTopic = "windsync/result",
            imageTopic = "windsync/image",
            onResult = { handleResult(it) },
            onStatus = { showStatus(it) }
        )

        mqttManager.connect()

        nav.btnNavCamera.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        nav.btnNavAdd.setOnClickListener {
            startActivity(Intent(this, MessageActivity::class.java))
        }

        nav.btnNavMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java) )
        }
        binding.btnGenerateEvent.setOnClickListener {
            generateRandomExtremeEvent()
        }



        loadEvents()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
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

            val json = res.getOrNull().orEmpty()
            allEvents = parseEvents(json)
            adapter.updateData(allEvents)
        }
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

    private fun showStatus(text: String) {
        runOnUiThread {
            binding.txtResult.text = text
        }
    }

    private fun handleResult(payload: String) {
        try {
            val json = JSONObject(payload)
            val isWind = json.getBoolean("is_wind_turbine")
            val confidence = json.getDouble("confidence")

            val text = if (!isWind) {
                "❌ NI vetrnica\nZanesljivost: ${(confidence * 100).toInt()} %"
            } else {
                val blades = json.getString("blades")
                val bladesConf = json.getDouble("blades_confidence")

                "✅ JE vetrnica\n" +
                        "Krakov: $blades\n" +
                        "Zanesljivost: ${(confidence * 100).toInt()} %\n" +
                        "Krakov conf: ${(bladesConf * 100).toInt()} %"
            }

            showStatus(text)
        } catch (_: Exception) {
            showStatus("❌ Napaka pri branju rezultata")
        }
    }

    private val extremeTopics = listOf(
        "veter/prevec",
        "veter/premalo",
        "veternica/okvara",
        "veternica/servis"
    )

    private fun randomSloveniaLocation(): Pair<Double, Double> {
        val lat = (45.4..46.9).random()
        val lon = (13.3..16.6).random()
        return lat to lon
    }

    private fun generateRandomExtremeEvent() {
        val topic = extremeTopics.random()

        val message = when (topic) {
            "veter/prevec" -> "Ekstremen veter zaznan"
            "veter/premalo" -> "Nenavadno mirno – ni vetra"
            "veternica/okvara" -> "Napaka na vetrnici"
            "veternica/servis" -> "Vetrnica potrebuje servis"
            else -> "Ekstremen dogodek"
        }

        val (lat, lon) = randomSloveniaLocation()

        lifecycleScope.launch {
            val res = ApiService.postEvent(topic, message, lat, lon)
            if (res.isSuccess) {
                loadEvents()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Napaka pri generiranju dogodka",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun ClosedFloatingPointRange<Double>.random() =
        start + Math.random() * (endInclusive - start)



    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        } else {
            openCamera()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(this, "Kamera ni dovoljena", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, TAKE_PHOTO)
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
        val bytes = stream.toByteArray()

        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        mqttManager.publishImage(base64)
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
    }
}
