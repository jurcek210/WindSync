package AST

data class Box(val from: String, val to: String) : Command {
    override fun eval(): GeoJsonFeature {
        val p1 = vars[from] as? Point ?: throw Error("Undefined or invalid point '$from'")
        val p2 = vars[to] as? Point ?: throw Error("Undefined or invalid point '$to'")

        val (x1, y1) = p1.eval()
        val (x2, y2) = p2.eval()

        val minX = minOf(x1, x2)
        val maxX = maxOf(x1, x2)
        val minY = minOf(y1, y2)
        val maxY = maxOf(y1, y2)

        val coordinates = listOf(
            listOf(minX, minY),
            listOf(minX, maxY),
            listOf(maxX, maxY),
            listOf(maxX, minY),
            listOf(minX, minY)
        )

        return GeoJsonFeature(
            geometry = GeoJsonGeometry(
                type = "Polygon",
                coordinates = listOf(coordinates)
            )
        )
    }
    fun evalArea(): Double {
        val p1 = vars[from] as? Point ?: throw Error("Invalid from point")
        val p2 = vars[to] as? Point ?: throw Error("Invalid to point")
        val (x1, y1) = p1.eval()
        val (x2, y2) = p2.eval()
        return Math.abs((x2 - x1) * (y2 - y1))
    }

    fun center(): Pair<Double, Double> {
        val p1 = vars[from] as? Point ?: throw Error("Undefined or invalid point '$from'")
        val p2 = vars[to] as? Point ?: throw Error("Undefined or invalid point '$to'")

        val (x1, y1) = p1.eval()
        val (x2, y2) = p2.eval()

        return Pair((x1 + x2) / 2.0, (y1 + y2) / 2.0)
    }
}
