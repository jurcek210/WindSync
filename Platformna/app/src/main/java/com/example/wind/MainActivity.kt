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
import org.eclipse.paho.client.mqttv3.*
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mqttClient: MqttClient

    private val REQUEST_CAMERA = 1
    private val TAKE_PHOTO = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MQTT
        val serverUri = "tcp://192.168.1.6:1883"
        val clientId = UUID.randomUUID().toString()
        mqttClient = MqttClient(serverUri, clientId, null)

        thread {
            try {
                mqttClient.connect(MqttConnectOptions().apply {
                    isCleanSession = true
                })

                runOnUiThread {
                    Toast.makeText(this, "MQTT povezan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "MQTT napaka: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnTestApi.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
    }


    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
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

        if (requestCode == REQUEST_CAMERA && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
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
        thread {
            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val bytes = stream.toByteArray()

                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                mqttClient.publish(
                    "windsync/image",
                    MqttMessage(base64.toByteArray())
                )

                runOnUiThread {
                    Toast.makeText(this, "Slika poslana", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Napaka: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mqttClient.isConnected) mqttClient.disconnect()
        } catch (_: Exception) {}
    }
}
