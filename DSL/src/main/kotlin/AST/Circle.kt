import AST.*


class Circle(val center: String, val radius: Expr) : Command {
    override fun eval(): GeoJsonFeature {
        val centerPoint = vars[center] as? Point ?: throw Error("Invalid center point '$center'")
        val (cx, cy) = centerPoint.eval()
        val r = radius.eval()


        val segments = 32
        val coords = (0..segments).map {
            val angle = 2 * Math.PI * it / segments
            listOf(
                cx + r * Math.cos(angle),
                cy + r * Math.sin(angle)
            )
        }

        return GeoJsonFeature(
            geometry = GeoJsonGeometry(
                type = "Polygon",
                coordinates = listOf(coords)
            )
        )
    }
}
