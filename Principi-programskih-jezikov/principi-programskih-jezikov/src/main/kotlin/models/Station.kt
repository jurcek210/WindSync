package models

import api.WeatherApi
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.*
import java.util.Locale

class Station(
    val id: UUID = UUID.randomUUID(),
    var name: String = "",
    val location: Location,
    var windSpeed: Double? = null,
    var status: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    weatherApi: WeatherApi
) {
    init {
        runBlocking {
            val enriched = weatherApi.enrichStation(this@Station)
            this@Station.name = enriched.name
            this@Station.windSpeed = enriched.windSpeed
        }
    }

    fun toggleStatus() {
        status = !status
    }

    fun refreshWindSpeed(weatherApi: WeatherApi) {
        runBlocking {
            val refreshed = weatherApi.enrichStation(this@Station)
            this@Station.windSpeed = refreshed.windSpeed
        }
    }

    fun printDetails() {
        println(toString())
    }

    override fun toString(): String {
        val lon = String.format(Locale.US, "%.6f", location.coordinates[0])
        val lat = String.format(Locale.US, "%.6f", location.coordinates[1])
        val wind = windSpeed?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"
        return "'$name' Lokacija($lon, $lat), wind: $wind m/s"
    }
}
