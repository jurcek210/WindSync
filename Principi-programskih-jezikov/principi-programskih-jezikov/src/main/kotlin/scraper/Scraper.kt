package scraper.thewindpower

import datamodels.ScrapedWindFarm
import models.Location
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Scraper {

    private const val BASE_URL = "https://www.thewindpower.net"

    fun scrapeWindFarms(): List<ScrapedWindFarm> {
        val farms = mutableListOf<ScrapedWindFarm>()
        val visitedNames = mutableSetOf<String>()

        val doc: Document = try {
            Jsoup.connect("$BASE_URL/country_windfarms_en_100_slovenia.php").get()
        } catch (e: Exception) {
            println("Napaka pri povezavi do osnovne strani: ${e.message}")
            return farms // vrni prazen seznam, ker ni mogoče pridobiti podatkov
        }

        val uniqueLinks = doc.select("a.lien_standard_tab")
            .map { it.attr("href") to it.text().trim() }
            .distinctBy { it.first }
            .map { (href, name) -> name to "$BASE_URL/$href" }

        for ((name, fullUrl) in uniqueLinks) {
            if (name in visitedNames) continue
            visitedNames.add(name)

            val document: Document = try {
                Jsoup.connect(fullUrl).get()
            } catch (e: Exception) {
                println("Napaka pri povezavi do strani vetrnice '$name': ${e.message}")
                continue // preskoči to vetrnico in nadaljuj
            }

            val farmDataList = extractWindFarmData(document, name)
            farms.addAll(farmDataList)
        }

        return farms
    }

    private fun extractWindFarmData(doc: Document, name: String): List<ScrapedWindFarm> {
        val liElements = doc.select("li.puce_texte").toList()

        val latitudes = liElements
            .filter { it.text().contains("Latitude:") }
            .map { it.text().substringAfter("Latitude:").trim() }

        val longitudes = liElements
            .filter { it.text().contains("Longitude:") }
            .map { it.text().substringAfter("Longitude:").trim() }

        val count = minOf(latitudes.size, longitudes.size)
        val farms = mutableListOf<ScrapedWindFarm>()

        for (i in 0 until count) {
            val lat = parseCoordinate(latitudes[i])
            val lon = parseCoordinate(longitudes[i])

            if (lat != null && lon != null) {
                val partName = if (count > 1) "$name - Part ${i + 1}" else name
                farms.add(ScrapedWindFarm(partName, Location.fromLatLon(lat, lon)))
            }
        }

        return farms
    }

    private fun parseCoordinate(dms: String): Double? {
        val cleaned = dms
            .replace("°", " ")
            .replace("'", " ")
            .replace("′", " ")
            .replace("\"", " ")
            .replace("″", " ")
            .replace(",", ".")
            .trim()

        val parts = cleaned.split(Regex("\\s+"))
        if (parts.size < 3) return null

        val degrees = parts[0].toDoubleOrNull() ?: return null
        val minutes = parts[1].toDoubleOrNull() ?: return null
        val seconds = parts[2].toDoubleOrNull() ?: return null

        return degrees + (minutes / 60) + (seconds / 3600)
    }
}
