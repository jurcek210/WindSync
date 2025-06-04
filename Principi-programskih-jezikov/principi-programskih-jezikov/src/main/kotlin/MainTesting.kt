import kotlinx.coroutines.runBlocking
import models.Location
import models.User

fun main() = runBlocking {
    // 1. Ustvari uporabnika
    val user = User(username = "Ana", email = "ana@example.com", password = "skrivnost")

    // 2. Dodaj 3 veternice z naključnimi lokacijami in naključnim vetrom
    user.stations.addRandomStations(count = 3, minWind = 1.0, maxWind = 12.0)

    // 3. Dodaj 2 veternici z naključnimi lokacijami in dejanskim vetrom (iz API)
    user.stations.addStationsOnRandomLocations(count = 2)

    // 4. Uporabi lokacijo Ljubljane kot center za območje (npr. 20 km)
    val ljubljana = Location.fromLatLon(46.0569, 14.5058)

    // 5. Dodaj 3 veternice v bližini Ljubljane z naključnim vetrom
    user.stations.addRandomStationsNear(center = ljubljana, radiusKm = 20.0, count = 3)

    // 6. Dodaj 2 veternici v bližini Ljubljane z realnimi podatki (API)
    user.stations.addStationsNear(center = ljubljana, radiusKm = 20.0, count = 2)

    // 7. Dodaj poskrapane slovenske veternice
    user.stations.addRealSlovenianStations()

    // 7. Izpiši vse veternice uporabnika
    println("\nSeznam vseh veternic uporabnika ${user.username}:")
    user.stations.printAll()

    // 8. Poišči najbližjo veternico neki lokaciji
    val testLocation = Location.fromLatLon(46.05, 14.49)
    val closest = user.stations.getClosestTo(testLocation)
    println("\nNajbližja veternica lokaciji (46.05, 14.49):")
    println(closest)
}
