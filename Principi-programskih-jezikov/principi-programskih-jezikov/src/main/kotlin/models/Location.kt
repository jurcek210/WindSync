package models

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
class Location(
    val coordinates: List<Double> = generateRandomSlovenianCoordinates()
) {
    val type: String = "Point"

    companion object {
        fun fromLatLon(lat: Double, lon: Double): Location {
            return Location(coordinates = listOf(lon, lat))
        }

        private fun generateRandomSlovenianCoordinates(): List<Double> {
            val lon = Random.nextDouble(13.375, 16.611)
            val lat = Random.nextDouble(45.421, 46.876)
            return listOf(lon, lat)
        }

        fun near(center: Location, radiusKm: Double): Location {
            val earthRadius = 6371.0

            val lat = center.getLatitude()
            val lon = center.getLongitude()

            val radiusInDegrees = radiusKm / earthRadius

            val u = Random.nextDouble()
            val v = Random.nextDouble()
            val w = radiusInDegrees * Math.sqrt(u)
            val t = 2 * Math.PI * v

            val deltaLat = w * Math.cos(t)
            val deltaLon = w * Math.sin(t) / Math.cos(Math.toRadians(lat))

            val newLat = lat + Math.toDegrees(deltaLat)
            val newLon = lon + Math.toDegrees(deltaLon)

            return fromLatLon(newLat, newLon)
        }
    }
    fun getLatitude(): Double = coordinates[1]
    fun getLongitude(): Double = coordinates[0]
}
