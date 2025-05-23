package AST


data class Line(val from: String, val to: String) : Command {
    override fun eval(): GeoJsonFeature {
        val fromPoint = vars[from] as? Point ?: throw Error("Undefined or invalid point '$from'")
        val toPoint = vars[to] as? Point ?: throw Error("Undefined or invalid point '$to'")

        return GeoJsonFeature(
            type = "Feature",
            geometry = GeoJsonGeometry(
                type = "LineString",
                coordinates = listOf(
                    listOf(fromPoint.x.eval(), fromPoint.y.eval()),
                    listOf(toPoint.x.eval(), toPoint.y.eval())
                )
            ),
            properties = emptyMap()
        )
    }


}