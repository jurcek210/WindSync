package models

import kotlinx.serialization.Serializable

@Serializable
class Location(
    val type: String = "Point",
    val coordinates: List<Double>
)