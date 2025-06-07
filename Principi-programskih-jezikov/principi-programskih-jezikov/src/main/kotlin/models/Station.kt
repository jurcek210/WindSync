package models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random
import java.util.Locale

@Serializable
data class Station(
    @Contextual val id: UUID = UUID.randomUUID(),
    var name: String,
    val location: Location,
    var windSpeed: Double? = null,
    var status: Boolean = true,
    var owner: String? = null,
    val measurements: List<String> = emptyList(),
    @Contextual val createdAt: LocalDateTime = LocalDateTime.now(),
    var windMillType: String? = null,
    var generated: Boolean = true   // tukaj dodamo polje, default true
) {
    fun toggleStatus() {
        status = !status
    }

    fun printDetails() {
        println(this.toString())
    }

    override fun toString(): String {
        val lon = String.format(Locale.US, "%.6f", location.getLongitude())
        val lat = String.format(Locale.US, "%.6f", location.getLatitude())
        val wind = windSpeed?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"
        return "'$name' Lokacija(lat: $lat, lon: $lon), wind: $wind m/s, generated: $generated, type: $windMillType"
    }

    private fun randomizeWindSpeed(min: Double, max: Double) {
        windSpeed = Random.nextDouble(min, max)
    }

    companion object {
        private val windMillTypes = listOf("tipA", "tipB", "tipC", "tipY", "tipZ", "tipW")
        private val windMillNamePrefixes = listOf(
            "Vetra",
            "Gora",
            "Bela",
            "Sonce",
            "Zora",
            "Moc",
            "Plamen",
            "Val",
            "Nebesa",
            "Sijaj",
            "Tiha",
            "Orkan",
            "Iskra",
            "Blisk",
            "Luna"
        )

        private val windMillNameSuffixes = listOf(
            "1",
            "2",
            "X",
            "Nova",
            "Prime",
            "Max",
            "Aurora",
            "Echo",
            "Vihar",
            "Zvezda",
            "Legenda",
            "Spektar",
            "Brezmejna",
            "Svetloba",
            "Horizont"
        )
        fun randomWindMillType(): String {
            return windMillTypes.random()
        }

        fun randomStationName(): String {
            val prefix = windMillNamePrefixes.random()
            val suffix = windMillNameSuffixes.random()
            return "$prefix$suffix"
        }

        suspend fun makeStation(
            name: String? = null,
            location: Location? = null,
            owner: String? = null,
            windMillType: String? = null
        ): Station {
            val loc = location ?: Location()
            val type = windMillType ?: randomWindMillType()
            val stationName = name ?: randomStationName()
            return Station(name = stationName, location = loc, owner = owner, windMillType = type, generated = true)
        }

        fun randomWind(
            name: String? = null,
            min: Double,
            max: Double,
            location: Location? = null,
            owner: String? = null,
            windMillType: String? = null
        ): Station {
            val loc = location ?: Location()
            val type = windMillType ?: randomWindMillType()
            val stationName = name ?: randomStationName()
            val station = Station(name = stationName, location = loc, owner = owner, windMillType = type, generated = true)
            station.randomizeWindSpeed(min, max)
            return station
        }
    }
}
