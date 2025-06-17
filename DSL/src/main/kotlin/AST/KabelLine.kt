package AST

data class KabelSegment(val from: Point, val to: Point, val radius: Double? = null) {

    fun toCoordinates(): Pair<Coordinates, Coordinates> =
        Coordinates(from.x.eval(), from.y.eval()) to Coordinates(to.x.eval(), to.y.eval())

    fun curvePoints(resolution: Int = 30): List<Coordinates> {
        val (c0, c1) = toCoordinates()
        return if (radius != null && radius > 0.0) {
            val bezier = Bezier.bend(c0, c1, radius)
            bezier.toPoints(resolution)
        } else {
            listOf(c0, c1)
        }
    }
}

