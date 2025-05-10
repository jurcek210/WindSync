package models

import java.util.*

class User(
    val id: UUID = UUID.randomUUID(),
    val username: String,
    val email: String,
    private var password: String,
    val stations: MutableList<Station> = mutableListOf()
) {

    fun addStation(station: Station) {
        stations.add(station)
    }

    fun removeStation(station: Station) {
        stations.remove(station)
    }

    fun printAllStations() {
        if (stations.isEmpty()) {
            println("$username nima nobene vremenske postaje.")
        } else {
            println("Postaje uporabnika $username:")
            stations.forEachIndexed { index, station ->
                println("${index + 1}. ${station}")
            }
        }
    }

    override fun toString(): String {
        return "$username <$email> (${stations.size} postaj)"
    }
}
