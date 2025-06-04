package models

import datamodels.ScrapedWindFarm
import io.github.serpro69.kfaker.Faker
import scraper.thewindpower.Scraper
import java.util.*

class Stations(
    private val list: MutableList<Station> = mutableListOf()
) {
    fun add(station: Station) {
        list.add(station)
    }

    fun deleteById(id: UUID): Boolean {
        val toRemove = list.find { it.id == id }
        return if (toRemove != null) {
            list.remove(toRemove)
            true
        } else {
            false
        }
    }

    fun getById(id: UUID): Station? {
        return list.find { it.id == id }
    }

    fun getActive(): List<Station> {
        return list.filter { it.status }
    }

    fun printAll() {
        if (list.isEmpty()) {
            println("Ni veternic.")
        } else {
            println("Seznam veternic:")
            list.forEachIndexed { index, station ->
                println("${index + 1}. $station")
            }
        }
    }

    fun getClosestTo(location: Location): Station? {
        if (list.isEmpty()) return null

        return list.minByOrNull { station ->
            val dx = station.location.getLongitude() - location.getLongitude()
            val dy = station.location.getLatitude() - location.getLatitude()
            dx * dx + dy * dy
        }
    }

    private val faker = Faker()

    fun addRandomStations(count: Int, minWind: Double = 0.0, maxWind: Double = 10.0) {
        repeat(count) {
            val name = faker.funnyName.name()
            val station = Station.randomWind(
                name = name,
                min = minWind,
                max = maxWind
            )
            add(station)
        }
    }

    suspend fun addStationsOnRandomLocations(count: Int) {
        repeat(count) {
            val name = faker.funnyName.name()
            val station = Station.makeStation(
                name = name
            )
            add(station)
        }
    }

    fun addRandomStationsNear(
        center: Location,
        radiusKm: Double,
        count: Int,
        minWind: Double = 0.0,
        maxWind: Double = 10.0
    ) {
        repeat(count) {
            val name = faker.funnyName.name()
            val nearbyLocation = Location.near(center, radiusKm)
            val station = Station.randomWind(
                name = name,
                min = minWind,
                max = maxWind,
                location = nearbyLocation
            )
            add(station)
        }
    }

    suspend fun addStationsNear(
        center: Location,
        radiusKm: Double,
        count: Int
    ) {
        repeat(count) {
            val name = faker.funnyName.name()
            val nearbyLocation = Location.near(center, radiusKm)
            val station = Station.makeStation(
                name = name,
                location = nearbyLocation
            )
            add(station)
        }
    }

    suspend fun addRealSlovenianStations() {
        val farms = Scraper.scrapeWindFarms()
        for (farm in farms) {
            val station = Station.makeStation(
                name = farm.name,
                location = farm.location
            )
            add(station)
        }
    }

    fun size(): Int = list.size

    fun getAll(): List<Station> = list
}