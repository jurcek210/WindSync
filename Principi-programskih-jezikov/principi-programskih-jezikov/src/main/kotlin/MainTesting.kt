import models.Location
import models.Station

fun main() {
    val ljubljana = Location.fromLatLon(46.0569, 14.5058)
    val stations = mutableListOf<Station>()

    repeat(1) {
        val nearLoc = Location.near(ljubljana, 10.0)
        val station = Station.randomWind("Station23 $it", 1.0, 10.0, nearLoc)
        stations.add(station)
    }

    // Shrani vse postaje naenkrat (batch insert)
    Database.windmills.insertMany(stations)

    println("${stations.size} stations saved to DB.")
}
