package com.example.wind

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wind.databinding.ActivitySimulationBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SimulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationBinding
    private var simulationJob: Job? = null

    private val extremeTopics = listOf(
        "veter/prevec",
        "veter/premalo",
        "veternica/okvara",
        "veternica/servis"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupIntervalSpinner()

        binding.switchSimulation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startSimulation()
            } else {
                stopSimulation()
            }
        }

        binding.btnGenerateExtreme.setOnClickListener {
           generateRandomExtremeEvent()
        }
    }

    private fun setupIntervalSpinner() {
        val intervals = listOf(
            "5 sekund",
            "10 minut",
            "30 minut"
        )

        binding.spinnerInterval.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            intervals
        )
    }

    private fun startSimulation() {
        stopSimulation()

        val intervalMs = when (binding.spinnerInterval.selectedItemPosition) {
            0 -> 5 *  1000L
            1 -> 10 * 60 * 1000L
            else -> 30 * 60 * 1000L
        }

        simulationJob = lifecycleScope.launch {
            while (true) {
                simulateWindCheck()
                delay(intervalMs)
            }
        }

        Toast.makeText(this, "Simulacija zagnana", Toast.LENGTH_SHORT).show()
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private suspend fun simulateWindCheck() {
        val windSpeed = (0..35).random()

        val topic = when {
            windSpeed > 25 -> "veter/prevec"
            windSpeed < 3 -> "veter/premalo"
            else -> null
        } ?: return

        val message = "Simulacija: hitrost vetra $windSpeed m/s"
        val (lat, lon) = randomSloveniaLocation()

        val res = ApiService.postEvent(topic, message, lat, lon)
        if (res.isSuccess) {
            runOnUiThread {
                val line =
                    "• ${System.currentTimeMillis()} | $topic | $windSpeed m/s\n"

                binding.tvSimulationLog.append(line)

                Toast.makeText(
                    this,
                    "Ekstrem zaznan ($windSpeed m/s)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
                Toast.makeText(
                    this@SimulationActivity,
                    "Dogodek ustvarjen: $topic",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@SimulationActivity,
                    "Napaka pri ustvarjanju dogodka",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    private fun randomSloveniaLocation(): Pair<Double, Double> {
        val lat = 45.4 + Math.random() * (46.9 - 45.4)
        val lon = 13.3 + Math.random() * (16.6 - 13.3)
        return lat to lon
    }

    override fun onDestroy() {
        stopSimulation()
        super.onDestroy()
    }
}
