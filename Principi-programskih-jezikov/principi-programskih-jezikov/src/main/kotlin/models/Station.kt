package models

import api.WeatherApi
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.*
import java.util.Locale
import kotlin.random.Random

class Station(
    val id: UUID = UUID.randomUUID(),
    var name: String,
    val location: Location,
    var windSpeed: Double? = null,
    var status: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toggleStatus() {
        status = !status
    }


    fun printDetails() {
        println(toString())
    }

    override fun toString(): String {
        val lon = String.format(Locale.US, "%.6f", location.getLongitude())
        val lat = String.format(Locale.US, "%.6f", location.getLatitude())
        val wind = windSpeed?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"
        return "'$name' Lokacija(lat: $lat, lon: $lon), wind: $wind m/s"
    }

    private fun randomizeWindSpeed(min: Double, max: Double) {
        windSpeed = Random.nextDouble(min, max)
    }

    companion object {

        suspend fun makeStation(name: String, location: Location? = null): Station {
            val loc = location ?: Location()
            val station = Station(name = name, location = loc)
            val enriched = api.openweather.OpenWeatherApi.enrichStation(station)
            return enriched
        }

        fun randomWind(name: String, min: Double, max: Double, location: Location? = null): Station {
            val loc = location ?: Location()
            val station = Station(name = name, location = loc)
            station.randomizeWindSpeed(min, max)
            return station
        }
    }
}