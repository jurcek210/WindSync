package api.openweather

import api.WeatherApi
import datamodels.WeatherResponse
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import models.Station

object OpenWeatherApi : WeatherApi {
    private val dotenv = dotenv()
    private val API_KEY = dotenv["OPENWEATHER_API_KEY"] ?: error("API key is missing")
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override suspend fun enrichStation(station: Station): Station {
        val response = client.get(BASE_URL) {
            parameter("lat", station.location.getLatitude())
            parameter("lon", station.location.getLongitude())
            parameter("appid", API_KEY)
            parameter("units", "metric")
        }.body<WeatherResponse>()

        station.windSpeed = response.wind.speed

        return station
    }
}
