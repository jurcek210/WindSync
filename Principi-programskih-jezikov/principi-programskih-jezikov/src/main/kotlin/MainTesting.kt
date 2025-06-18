import scraper.thewindpower.Scraper

fun main() {
    // Uporabnik lahko nastavi del URL-ja po potrebi
    val relativePath = "country_windfarms_en_13_austria.php"
    val windFarms = Scraper.scrapeWindFarms(relativePath)

    windFarms.forEach {
        println("${it.name}: ${it.location.getLatitude()}  ${it.location.getLongitude()}, power: ${it.power}, status: ${it.status}")
    }
}
