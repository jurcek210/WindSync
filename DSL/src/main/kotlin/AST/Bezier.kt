package AST

import kotlin.math.*

data class Coordinates(val x: Double, val y: Double) {
    operator fun plus(other: Coordinates) = Coordinates(x + other.x, y + other.y)
    operator fun minus(other: Coordinates) = Coordinates(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Coordinates(x * scalar, y * scalar)
    operator fun div(scalar: Double) = Coordinates(x / scalar, y / scalar)
    fun dist(other: Coordinates): Double {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
    fun angle(other: Coordinates): Double = atan2(other.y - y, other.x - x)
    fun offset(dist: Double, angle: Double): Coordinates {
        val dx = dist * cos(angle)
        val dy = dist * sin(angle)
        return Coordinates(x + dx, y + dy)
    }
    operator fun times(scalar: Int) = this * scalar.toDouble()
    operator fun times(scalar: Float) = this * scalar.toDouble()
    override fun toString() = "Coordinates(x=$x, y=$y)"
}

class Bezier(private val p0: Coordinates, private val p1: Coordinates, private val p2: Coordinates, private val p3: Coordinates) {
    fun at(t: Double): Coordinates =
        p0 * (1.0 - t).pow(3.0) +
                p1 * 3.0 * (1.0 - t).pow(2.0) * t +
                p2 * 3.0 * (1.0 - t) * t.pow(2.0) +
                p3 * t.pow(3.0)

    fun toPoints(segmentsCount: Int): List<Coordinates> =
        (0..segmentsCount).map { at(it / segmentsCount.toDouble()) }

    fun approxLength(): Double {
        val midpoint = at(0.5)
        return p0.dist(midpoint) + midpoint.dist(p3)
    }

    fun resolutionToSegmentsCount(resolution: Double): Int =
        (resolution * approxLength()).coerceAtLeast(2.0).toInt()

    companion object {
        fun bend(t0: Coordinates, t1: Coordinates, relativeAngleDegrees: Double): Bezier {
            val relativeAngle = Math.toRadians(relativeAngleDegrees)
            val oppositeRelativeAngle = Math.PI - relativeAngle

            val angle = t0.angle(t1)
            val dist = t0.dist(t1)
            val constant = (4 / 3) * tan(Math.PI / 8)

            val c0 = t0.offset(constant * dist, angle + relativeAngle)
            val c1 = t1.offset(constant * dist, angle + oppositeRelativeAngle)

            return Bezier(t0, c0, c1, t1)
        }
    }
}
