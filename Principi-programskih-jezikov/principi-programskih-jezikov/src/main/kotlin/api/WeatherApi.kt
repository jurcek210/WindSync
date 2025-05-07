package api

import models.Station
import models.Location

interface WeatherApi {
    suspend fun enrichStation(station: Station): Station
}