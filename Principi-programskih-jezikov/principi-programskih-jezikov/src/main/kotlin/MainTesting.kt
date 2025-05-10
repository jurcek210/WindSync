import api.openweather.OpenWeatherApi
import kotlinx.coroutines.runBlocking
import models.Location
import models.Station
import models.User

fun main() = runBlocking {
    // 1. Ustvari uporabnika
    val user = User(username = "ana", email = "ana@example.com", password = "tajno")

    // 2. Lokacije
    val location1 = Location(coordinates = listOf(15.6467, 46.5547))
    val location2 = Location(coordinates = listOf(14.907955, 46.332981))

    // 3. Postaje
    val station1 = Station(location = location1, weatherApi = OpenWeatherApi)
    val station2 = Station(location = location2, weatherApi = OpenWeatherApi)

    // 4. Dodaj postaji uporabniku
    user.addStation(station1)
    user.addStation(station2)

    // 5. Izpiši uporabnika in vse njegove postaje
    println("Uporabnik:")
    println(user)
    println()

    user.printAllStations()
}
