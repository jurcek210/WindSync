import AST.*
data class Connect(val from: String, val to: String) : Command {
    override fun eval(): GeoJsonFeature {
        val fromEntity = vars[from]
        val toEntity = vars[to]

        val p1 = when (fromEntity) {
            is Point -> fromEntity.eval()
            else -> throw Error("Invalid or undefined point: $from")
        }

        val p2 = when (toEntity) {
            is Point -> toEntity.eval()
            else -> throw Error("Invalid or undefined point: $to")
        }

        return GeoJsonFeature(
            geometry = GeoJsonGeometry(
                type = "LineString",
                coordinates = listOf(
                    listOf(p1.first, p1.second),
                    listOf(p2.first, p2.second)
                )
            ),
            properties = mapOf("type" to "connection", "from" to from, "to" to to)
        )
    }
}
