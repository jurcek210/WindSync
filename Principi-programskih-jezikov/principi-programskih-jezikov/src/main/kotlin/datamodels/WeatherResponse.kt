package datamodels

import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val name: String,
    val coord: Coord,
    val wind: Wind
)
