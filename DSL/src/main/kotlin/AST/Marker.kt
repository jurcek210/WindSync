import AST.*
data class Marker(val pointName: String, val props: MutableMap<String, Any> = mutableMapOf()) : Command {
    override fun eval(): GeoJsonFeature {
        val point = vars[pointName] as? Point ?: throw Error("Invalid point for marker: $pointName")
        val (x, y) = point.eval()
        println("Marker '$pointName' eval with properties: $props")

        return GeoJsonFeature(
            geometry = GeoJsonGeometry(
                type = "Point",
                coordinates = listOf(x, y)
            ),
            properties = props
        )
    }
}

