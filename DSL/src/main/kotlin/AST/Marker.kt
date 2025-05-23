import AST.*
data class Marker(val pointName: String, val props: Map<String, Any>) : Command {
    override fun eval(): GeoJsonFeature {
        val point = vars[pointName] as? Point ?: throw Error("Invalid point for marker: $pointName")
        val (x, y) = point.eval()

        return GeoJsonFeature(
            geometry = GeoJsonGeometry(
                type = "Point",
                coordinates = listOf(x, y)
            ),
            properties = props
        )
    }
}
