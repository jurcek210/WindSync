package api.openweather.models

import kotlinx.serialization.Serializable
import api.openweather.models.Coord
import api.openweather.models.Wind

@Serializable
data class WeatherResponse(
    val name: String,
    val coord: Coord,
    val wind: Wind
)
