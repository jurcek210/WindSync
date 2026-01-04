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
import com.example.wind.databinding.ActivityMainBinding
import com.example.wind.mqtt.MqttManager
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mqttManager: MqttManager

    private val REQUEST_CAMERA = 1
    private val TAKE_PHOTO = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTestApi.isEnabled = false

        mqttManager = MqttManager(
            serverUri = "tcp://192.168.1.6:1883",
            resultTopic = "windsync/result",
            imageTopic = "windsync/image",
            onResult = { handleResult(it) },
            onStatus = { updateStatus(it) }
        )

        mqttManager.connect()

        binding.btnTestApi.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
    }

    private fun updateStatus(text: String) {
        runOnUiThread {
            binding.txtResult.text = text
            if (text.contains("MQTT povezan")) {
                binding.btnTestApi.isEnabled = true
            }
        }
    }

    // ===============================
    // OBDELAVA REZULTATA
    // ===============================
    private fun handleResult(payload: String) {
        try {
            val json = JSONObject(payload)
            val isWind = json.getBoolean("is_wind_turbine")
            val confidence = json.getDouble("confidence")

            val resultText = if (!isWind) {
                "❌ NI vetrnica\nZanesljivost: ${(confidence * 100).toInt()} %"
            } else {
                val blades = json.getString("blades")
                val bladesConf = json.getDouble("blades_confidence")

                "✅ JE vetrnica\n" +
                        "Krakov: $blades\n" +
                        "Zanesljivost: ${(confidence * 100).toInt()} %\n" +
                        "Krakov conf: ${(bladesConf * 100).toInt()} %"
            }

            updateStatus(resultText)

        } catch (e: Exception) {
            updateStatus("❌ Napaka pri branju rezultata")
        }
    }

    // ===============================
    // KAMERA
    // ===============================
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
