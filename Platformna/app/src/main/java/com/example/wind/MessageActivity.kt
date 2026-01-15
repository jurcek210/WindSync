package com.example.wind

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.wind.databinding.ActivityMessageBinding
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageBinding
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val requestLocation =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!ok) Toast.makeText(this, "Lokacija ni dovoljena", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isEmpty()) {
                Toast.makeText(this, "Vnesi sporočilo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ensureLocationAndSend(msg)
        }
    }

    private fun setupSpinners() {
        val categories = listOf("Veter", "Vetrnica")
        binding.spinnerCategory.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        fun setSub(items: List<String>) {
            binding.spinnerSubcategory.adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        }

        setSub(listOf("prevec", "premalo"))

        binding.spinnerCategory.setOnItemSelectedListener { _, _, pos, _ ->
            if (categories[pos] == "Veter") setSub(listOf("prevec", "premalo"))
            else setSub(listOf("servis", "okvara"))
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

    private fun ensureLocationAndSend(message: String) {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            requestLocation.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            Toast.makeText(this, "Dovoli lokacijo in klikni Pošlji še enkrat.", Toast.LENGTH_SHORT).show()
            return
        }

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                Toast.makeText(this, "Lokacije ni mogoče dobiti.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val topic = buildTopic()
            lifecycleScope.launch {
                val result = ApiService.postEvent(
                    topic = topic,
                    message = message,
                    lat = loc.latitude,
                    lon = loc.longitude
                )

                if (result.isSuccess) {
                    Toast.makeText(this@MessageActivity, "Dogodek poslan", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@MessageActivity,
                        "Napaka: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun buildTopic(): String {
        val cat = binding.spinnerCategory.selectedItem.toString()
        val sub = binding.spinnerSubcategory.selectedItem.toString()
        val catSlug = if (cat == "Veter") "veter" else "veternica"
        return "$catSlug/$sub"
    }
}
