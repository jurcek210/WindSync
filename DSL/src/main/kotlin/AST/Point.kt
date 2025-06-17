package AST

data class Point(val x: Expr, val y: Expr) {
    fun eval(): Pair<Double, Double> = Pair(x.eval(), y.eval())

    override fun toString(): String = "point(${x}, ${y})"
}
