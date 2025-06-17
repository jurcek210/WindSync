package AST
import AST.*

import Circle
import Marker

val vars = mutableMapOf<String, Any>()
val props = mutableMapOf<String, Any>()


data class GetExpr(val key: String) : Expr {
    override fun eval(): Double {
        val v = props[key] ?: throw Error("No value found for key '$key'")
        return when (v) {
            is Number -> v.toDouble()
            is Expr -> v.eval()
            else -> throw Error("Value for '$key' is not numeric")
        }
    }

    override fun toString(): String = "get(\"$key\")"
}
data class StringLiteral(val value: String) : Expr {
    override fun eval(): Double {
        throw Error("Cannot evaluate string as number")
    }

    fun getString(): String = value
}

data class SetExpr(val fullKey: String, val value: Any) : Expr {
    override fun eval(): Double {
        val parts = fullKey.split(".")
        if (parts.size != 2) throw Error("Invalid property access: $fullKey")

        val entityName = parts[0]
        val property = parts[1]

        val target = vars[entityName] ?: throw Error("Entity '$entityName' not found")
        if (target is Marker) {
            target.props[property] = value
            println("? set('$fullKey') = $value")
            return 1.0
        } else {
            throw Error("set() only supports Marker for now, got ${target.javaClass.simpleName}")
        }
    }

    override fun toString(): String = "set(\"$fullKey\" to $value)"
}



interface Expr {
    fun eval(): Double
    override fun toString(): String
    data class Real(val value: Double) : Expr {
        override fun eval(): Double = value
        override fun toString(): String = value.toString()
    }




    data class CountExpr(val type: String) : Expr {
        override fun eval(): Double {
            return commands.count {
                when (type.lowercase()) {
                    "circle" -> it is Circle
                    "box" -> it is Box
                    "marker" -> it is Marker
                    "kabel" -> it is Kabel
                    "connect" -> it is Connect
                    else -> false
                }
            }.toDouble()
        }
    }


    data class AreaExpr(val names: List<String>) : Expr {
        override fun eval(): Double {
            return commands.sumOf {
                if (it is Box && names.contains(vars.filterValues { v -> v == it }.keys.firstOrNull())) {
                    it.evalArea()
                } else if (it is Circle && names.contains(vars.filterValues { v -> v == it }.keys.firstOrNull())) {
                    it.evalArea()
                } else 0.0
            }
        }
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
