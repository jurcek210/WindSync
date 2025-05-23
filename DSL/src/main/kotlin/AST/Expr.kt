package AST

val vars = mutableMapOf<String, Any>()


interface Expr {
    fun eval(): Double
    override fun toString(): String
    data class Real(val value: Double) : Expr {
        override fun eval(): Double = value
        override fun toString(): String = value.toString()
    }

    data class Variable2(val name: String) : Expr {
        override fun eval(): Double {
            val v = vars[name] ?: throw Error("Variable '$name' not defined")
            return when (v) {
                is Double -> v
                is Expr -> v.eval()
                else -> throw Error("Variable '$name' is not numeric")
            }
        }
        override fun toString(): String = name
    }

    data class UnaryMinus(val expr: Expr) : Expr {
        override fun eval(): Double = -expr.eval()
        override fun toString(): String = "-$expr"
    }




}

data class Plus(val left: Expr, val right: Expr) : Expr {
    override fun eval(): Double = left.eval() + right.eval()
    override fun toString() = "($left + $right)"
}

data class Minus(val left: Expr, val right: Expr) : Expr {
    override fun eval(): Double = left.eval() - right.eval()
    override fun toString() = "($left - $right)"
}

data class Times(val left: Expr, val right: Expr) : Expr {
    override fun eval(): Double = left.eval() * right.eval()
    override fun toString() = "($left * $right)"
}

data class Divides(val left: Expr, val right: Expr) : Expr {
    override fun eval(): Double = left.eval() / right.eval()
    override fun toString() = "($left / $right)"
}
