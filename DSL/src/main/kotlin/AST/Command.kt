package AST
data class GeoJsonFeature(
    val type: String = "Feature",
    val geometry: GeoJsonGeometry,
    val properties: Map<String, Any> = emptyMap()
)

data class GeoJsonGeometry(
    val type: String,
    val coordinates: Any
)

data class GeoJsonFeatureCollection(
    val type: String = "FeatureCollection",
    val features: List<GeoJsonFeature>
)

data class Kabel(val name: String, val segments: List<Pair<Point, Point>>) : Command {
    override fun eval(): GeoJsonFeature {
        val coords = segments.map { segment ->
            listOf(segment.first.x.eval(), segment.first.y.eval()) to
                    listOf(segment.second.x.eval(), segment.second.y.eval())
        }.map { listOf(it.first, it.second) } // pretvori v LineString

        return GeoJsonFeature(
            type = "Feature",
            geometry = GeoJsonGeometry(
                type = "MultiLineString",
                coordinates = coords
            ),
            properties = mapOf("type" to "kabel", "name" to name)
        )
    }
}




interface Command {
    fun eval(): GeoJsonFeature
}

val commands = mutableListOf<Command>()
