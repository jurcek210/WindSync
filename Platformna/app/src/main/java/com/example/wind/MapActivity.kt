package com.example.wind

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wind.databinding.ActivityMapBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: MapLibreMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.getMapAsync { m ->
            map = m
            map.setStyle(
                Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")
            ) { style ->
                style.addImage(
                    "icon-windmill",
                    androidx.core.content.ContextCompat.getDrawable(
                        this,
                        R.drawable.windmill_marker
                    )!!
                )
                loadEvents(style)
            }

            map.addOnMapClickListener {
                showAddEventDialog(it.latitude, it.longitude)
                true
            }
        }

        binding.bottomNav.btnNavMap.isSelected = true
        binding.bottomNav.btnNavAdd.setOnClickListener {
            map.cameraPosition = map.cameraPosition
        }
        binding.bottomNav.btnNavCamera.setOnClickListener {
            finish()
        }
    }

    private fun loadEvents(style: Style) {
        lifecycleScope.launch {
            val res = ApiService.getEvents(null)
            if (res.isFailure) return@launch
            val features = parseToFeatures(res.getOrNull().orEmpty())
            drawLayers(style, features)
        }
    }

    private fun parseToFeatures(json: String): List<Feature> {
        val arr = JSONArray(json)
        val out = mutableListOf<Feature>()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val topic = o.optString("topic")
            val coords = o.optJSONObject("location")?.optJSONArray("coordinates") ?: continue

            val lon = coords.optDouble(0)
            val lat = coords.optDouble(1)

            val type = when {
                topic.startsWith("veter/") -> "veter"
                topic.startsWith("veternica/") -> "veternica"
                else -> null
            } ?: continue

            val f = Feature.fromGeometry(Point.fromLngLat(lon, lat))
            f.addStringProperty("type", type)
            out.add(f)
        }
        return out
    }

    private fun drawLayers(style: Style, features: List<Feature>) {
        style.removeLayer("veter-layer")
        style.removeLayer("veternica-layer")
        style.removeSource("events-source")

        style.addSource(
            GeoJsonSource(
                "events-source",
                FeatureCollection.fromFeatures(features)
            )
        )

        style.addLayer(
            CircleLayer("veter-layer", "events-source")
                .withFilter(eq(get("type"), literal("veter")))
                .withProperties(
                    circleColor("#000000"),
                    circleRadius(6f)
                )
        )

        style.addLayer(
            SymbolLayer("veternica-layer", "events-source")
                .withFilter(eq(get("type"), literal("veternica")))
                .withProperties(
                    iconImage("icon-windmill"),
                    iconAllowOverlap(true),
                    iconOffset(arrayOf(0f, -40f)),
                    iconSize(
                        interpolate(
                            linear(), zoom(),
                            literal(6), literal(0.05f),
                            literal(10), literal(0.1f),
                            literal(14), literal(0.18f)
                        )
                    )
                )
        )

        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(46.064046, 14.692703))
            .zoom(7.0)
            .build()
    }

    private fun showAddEventDialog(lat: Double, lon: Double) {
        val topics = arrayOf(
            "veter/prevec",
            "veter/premalo",
            "veternica/okvara",
            "veternica/servis"
        )

        val dialogBinding =
            com.example.wind.databinding.DialogAddEventBinding.inflate(layoutInflater)

        dialogBinding.spinnerTopic.adapter =
            android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                topics
            )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Dodaj dogodek")
            .setView(dialogBinding.root)
            .setPositiveButton("Shrani") { _, _ ->
                val topic = dialogBinding.spinnerTopic.selectedItem.toString()
                val message = dialogBinding.etMessage.text.toString()
                if (message.isBlank()) return@setPositiveButton

                lifecycleScope.launch {
                    val res = ApiService.postEvent(topic, message, lat, lon)
                    if (res.isSuccess) loadEvents(map.style!!)
                }
            }
            .setNegativeButton("Prekliči", null)
            .show()
    }

    override fun onStart() { super.onStart(); binding.mapView.onStart() }
    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { binding.mapView.onPause(); super.onPause() }
    override fun onStop() { binding.mapView.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }
    override fun onDestroy() { binding.mapView.onDestroy(); super.onDestroy() }
}
