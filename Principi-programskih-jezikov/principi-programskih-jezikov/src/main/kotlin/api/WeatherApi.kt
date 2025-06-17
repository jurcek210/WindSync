package api

import models.*
import models.Location

interface WeatherApi {
    suspend fun enrichStation(station: Station): Station
}