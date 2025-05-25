package AST

import Circle

data class Connect(val from: String, val to: String, val bend: Double? = null) : Command
{
    override fun eval(): GeoJsonFeature {
        val fromEntity = vars[from]
        val toEntity = vars[to]

        val p1 = resolveCenter(from, fromEntity)
        val p2 = resolveCenter(to, toEntity)

        val coords = if (bend != null) {
            val start = Coordinates(p1.first, p1.second)
            val end = Coordinates(p2.first, p2.second)
            Bezier.bend(start, end, bend).toPoints(20).map { listOf(it.x, it.y) }
        } else {
            listOf(listOf(p1.first, p1.second), listOf(p2.first, p2.second))
        }

        return GeoJsonFeature(
            geometry = GeoJsonGeometry(
                type = "LineString",
                coordinates = coords
            ),
            properties = mapOf("type" to "connection", "from" to from, "to" to to)
        )
    }


    private fun resolveCenter(name: String, entity: Any?): Pair<Double, Double> {
        return when (entity) {
            is Point -> {
                println("? $name is a Point")
                entity.eval()
            }

            is Box -> {
                println("? $name is a Box")
                val p1 = vars[entity.from] as? Point
                    ?: throw Error("Invalid Box: '${entity.from}' is not a Point")
                val p2 = vars[entity.to] as? Point
                    ?: throw Error("Invalid Box: '${entity.to}' is not a Point")
                val (x1, y1) = p1.eval()
                val (x2, y2) = p2.eval()
                Pair((x1 + x2) / 2.0, (y1 + y2) / 2.0)
            }

            is Circle -> {
                println("? $name is a Circle")
                val centerPoint = vars[entity.center] as? Point
                    ?: throw Error("Invalid Circle center reference '${entity.center}'")
                centerPoint.eval()
            }

            else -> {
                println("? $name is unknown or unsupported: ${entity?.javaClass?.simpleName}")
                throw Error("Cannot resolve position of '$name' (unsupported type)")
            }
        }
    }

}
