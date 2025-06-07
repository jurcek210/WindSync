import scraper.thewindpower.Scraper

fun main() {
    val windFarms = Scraper.scrapeWindFarms()
    windFarms.forEach {
        println("${it.name}: ${it.location.getLatitude()}  ${it.location.getLongitude()}")
    }
}