package datamodels

import kotlinx.serialization.Serializable
import models.Location

@Serializable
data class ScrapedWindFarm(
    val name: String,
    val location: Location,
    val power: Int,
    val status: String
)