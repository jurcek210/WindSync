import AST.*
import org.example.Lexer


class Parser(
    private val lex: Lexer,
    private var currentToken: Token = lex.getToken()
) {

    fun parse() {
        try {
            if (currentToken.symbol == Symbol.REAL || currentToken.symbol == Symbol.LPAREN ||
                currentToken.symbol == Symbol.MINUS || currentToken.symbol == Symbol.PLUS ||
                currentToken.symbol == Symbol.VARIABLE) {

                val expr = additive()
                if (currentToken.symbol == Symbol.EOF) {
                    println("Evaluated expression: ${expr.eval()}")
                } else {
                    println("Unexpected token after expression: ${currentToken.symbol}")
                }

            } else {
                constructs()
                if (currentToken.symbol == Symbol.EOF) {
                    println("accept")
                } else {
                    println("reject")
                }
            }

        } catch (e: Exception) {
            println("reject")
            e.printStackTrace() // za lažje debugiranje
        }
    }




    private fun additive(): Expr {
        var left = multiplicative()
        while (currentToken.symbol == Symbol.PLUS || currentToken.symbol == Symbol.MINUS) {
            val op = currentToken.symbol
            currentToken = lex.getToken()
            val right = multiplicative()
            left = if (op == Symbol.PLUS) Plus(left, right) else Minus(left, right)
        }
        return left
    }


    private fun multiplicative(): Expr {
        var expr = unary()

        while (currentToken.symbol == Symbol.TIMES || currentToken.symbol == Symbol.DIVIDES) {
            val op = currentToken.symbol
            currentToken = lex.getToken()
            val right = unary()
            expr = when (op) {
                Symbol.TIMES -> Times(expr, right)
                Symbol.DIVIDES -> Divides(expr, right)
                else -> throw Error("Unexpected operator")
            }
        }

        return expr
    }


    private fun unary(): Expr {
        return when (currentToken.symbol) {
            Symbol.MINUS -> {
                currentToken = lex.getToken()
                Expr.UnaryMinus(primary())
            }
            Symbol.PLUS -> {
                currentToken = lex.getToken()
                primary()
            }
            else -> primary()
        }
    }


    private fun get(): Expr {
        if (currentToken.symbol != Symbol.LPAREN)
            throw Error("Expected '(' after get")

        currentToken = lex.getToken()

        if (currentToken.symbol != Symbol.STRING)
            throw Error("Expected string key in get(...)")

        val key = currentToken.lexeme
        currentToken = lex.getToken()

        if (currentToken.symbol != Symbol.RPAREN)
            throw Error("Expected ')' after get(...)")

        currentToken = lex.getToken()

        val value = if ('.' in key) {
            val (objectName, propertyName) = key.split('.', limit = 2)
            val obj = vars[objectName]
            if (obj is Marker) {
                obj.props[propertyName]
                    ?: throw Error("No property '$propertyName' in marker '$objectName'")
            } else {
                throw Error("No marker named '$objectName'")
            }
        } else {
            vars[key] ?: throw Error("No value stored under '$key'")
        }

        val result = when (value) {
            is Expr -> value
            is Double -> Expr.Real(value)
            is Int -> Expr.Real(value.toDouble())
            is Boolean -> Expr.Real(if (value) 1.0 else 0.0)
            is String -> StringLiteral(value)
            else -> throw Error("Unsupported value type in get('$key')")
        }

        println("✔ get('$key') = $value")
        return result
    }




    private fun areaArgs() {
        entity()
        areaArgs2()
    }
    private fun areaArgs2() {
        if (currentToken.symbol == Symbol.TO) {
            currentToken = lex.getToken()
            entity()
            areaArgs2()
        }
    }

    private fun entity() {
        if (currentToken.symbol == Symbol.STRING || currentToken.symbol == Symbol.VARIABLE) {
            currentToken = lex.getToken()
        } else {
            throw Error("Expected entity (string or variable), got ${currentToken.symbol} at ${currentToken.row}:${currentToken.column}")
        }
    }



    private fun primary(): Expr {
        return when (currentToken.symbol) {

            Symbol.REAL -> {
                val value = currentToken.lexeme.toDouble()
                currentToken = lex.getToken()
                Expr.Real(value)
            }

            Symbol.VARIABLE -> {
                val name = currentToken.lexeme
                currentToken = lex.getToken()
                Expr.Variable2(name)
            }

            Symbol.MINUS -> {
                currentToken = lex.getToken()
                val expr = primary()
                Expr.UnaryMinus(expr)
            }

            Symbol.LPAREN -> {
                currentToken = lex.getToken()
                val inner = additive()
                if (currentToken.symbol != Symbol.RPAREN) {
                    throw Error("Expected ')' at ${currentToken.row}:${currentToken.column}")
                }
                currentToken = lex.getToken()
                return inner
            }
            Symbol.COUNT -> {
                currentToken = lex.getToken()
                if (currentToken.symbol != Symbol.LPAREN) throw Error("Expected '(' after count")
                currentToken = lex.getToken()
                val type = if (currentToken.symbol == Symbol.STRING) {
                    val t = currentToken.lexeme
                    currentToken = lex.getToken()
                    t
                } else throw Error("Expected string argument in count")

                if (currentToken.symbol != Symbol.RPAREN) throw Error("Expected ')' after count")
                currentToken = lex.getToken()
                Expr.CountExpr(type)
            }

            Symbol.AREA -> {
                currentToken = lex.getToken()
                if (currentToken.symbol != Symbol.LPAREN) throw Error("Expected '(' after area")
                currentToken = lex.getToken()

                val names = mutableListOf<String>()

                while (currentToken.symbol == Symbol.STRING || currentToken.symbol == Symbol.VARIABLE) {
                    names.add(currentToken.lexeme)
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()
                    } else break
                }

                if (currentToken.symbol != Symbol.RPAREN) throw Error("Expected ')' after area(...)")
                currentToken = lex.getToken()

                return Expr.AreaExpr(names)
            }
            Symbol.GET -> {
                currentToken = lex.getToken()
                if (currentToken.symbol != Symbol.LPAREN) throw Error("Expected '(' after get")
                currentToken = lex.getToken()
                if (currentToken.symbol != Symbol.STRING) throw Error("Expected string as key in get")
                val key = currentToken.lexeme
                currentToken = lex.getToken()
                if (currentToken.symbol != Symbol.RPAREN) throw Error("Expected ')' after get")
                currentToken = lex.getToken()

                val v = vars[key] ?: throw Error("No value stored under '$key'")
                val value = when (v) {
                    is Expr -> v.eval()
                    is Double -> v
                    is Int -> v.toDouble()
                    is Boolean -> if (v) 1.0 else 0.0
                    is String -> {
                        println("✔ get('$key') = $v")
                        vars[key] = v
                        return StringLiteral(v)
                    }
                    else -> throw Error("Cannot get $v as number (key = '$key')")
                }

                println("✔ get('$key') = $value")
                return Expr.Real(value)
            }








            else -> throw Error("Unexpected token in primary: ${currentToken.symbol}")
        }
    }


    private fun properties() {
        property()
        while (currentToken.symbol == Symbol.TO) {
            currentToken = lex.getToken()
            property()
        }
    }

    private fun property() {
        when (currentToken.symbol) {
            Symbol.COLOR -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.ASSIGN) {
                    currentToken = lex.getToken()
                    color()
                } else {
                    throw Error("Expected '=' after color")
                }
            }
            Symbol.LABEL -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.ASSIGN) {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.STRING) {
                        currentToken = lex.getToken()
                    } else {
                        throw Error("Expected string after label=")
                    }
                } else {
                    throw Error("Expected '=' after label")
                }
            }
            Symbol.VALUE -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.ASSIGN) {
                    currentToken = lex.getToken()
                    primary()
                } else {
                    throw Error("Expected '=' after value")
                }
            }
            else -> throw Error("Expected property at ${currentToken.row}:${currentToken.column}")
        }
    }


    private fun expectAssign() {
        if (currentToken.symbol == Symbol.ASSIGN) {
            currentToken = lex.getToken()
        } else {
            throw Error("Expected '=' at ${currentToken.row}:${currentToken.column}")
        }
    }
    private fun color() {
        when (currentToken.symbol) {
            Symbol.RED, Symbol.GREEN, Symbol.BLUE, Symbol.YELLOW, Symbol.BLACK, Symbol.WHITE,
            Symbol.GRAY, Symbol.PURPLE, Symbol.ORANGE, Symbol.PINK, Symbol.BROWN,
            Symbol.CYAN, Symbol.MAGENTA -> {
                currentToken = lex.getToken()
            }
            Symbol.RGB -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()
                    val r = currentToken
                    if (r.symbol == Symbol.REAL) {
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.TO) {
                            currentToken = lex.getToken()
                            val g = currentToken
                            if (g.symbol == Symbol.REAL) {
                                currentToken = lex.getToken()
                                if (currentToken.symbol == Symbol.TO) {
                                    currentToken = lex.getToken()
                                    val b = currentToken
                                    if (b.symbol == Symbol.REAL) {
                                        currentToken = lex.getToken()
                                        if (currentToken.symbol == Symbol.RPAREN) {
                                            currentToken = lex.getToken()
                                            return
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                throw Error("Invalid rgb color syntax at ${currentToken.row}:${currentToken.column}")
            }
            else -> throw Error("Expected color at ${currentToken.row}:${currentToken.column}")
        }
    }

    private fun point(): Point {
        if (currentToken.symbol != Symbol.LPAREN) {
            throw Error("Expected '(' at start of point at ${currentToken.row}:${currentToken.column}")
        }
        currentToken = lex.getToken()

        val x = additive()

        if (currentToken.symbol != Symbol.TO) {
            throw Error("Expected ',' between coordinates at ${currentToken.row}:${currentToken.column}")
        }
        currentToken = lex.getToken()

        val y = additive()

        if (currentToken.symbol != Symbol.RPAREN) {
            throw Error("Expected ')' at end of point at ${currentToken.row}:${currentToken.column}")
        }
        currentToken = lex.getToken()

        return Point(x, y)
    }

    private fun set() {
        if (currentToken.symbol != Symbol.LPAREN)
            throw Error("Expected '(' at beginning of set()")

        currentToken = lex.getToken()

        if (currentToken.symbol != Symbol.STRING)
            throw Error("Expected string as property name")

        val key = currentToken.lexeme
        currentToken = lex.getToken()

        if (currentToken.symbol != Symbol.TO)
            throw Error("Expected ',' after key in set()")

        currentToken = lex.getToken()

        val value = when (currentToken.symbol) {
            Symbol.STRING -> {
                val v = currentToken.lexeme
                currentToken = lex.getToken()
                v
            }
            Symbol.TRUE -> {
                currentToken = lex.getToken()
                true
            }
            Symbol.FALSE -> {
                currentToken = lex.getToken()
                false
            }
            Symbol.VARIABLE -> {
                val name = currentToken.lexeme
                currentToken = lex.getToken()
                val stored = vars[name] ?: throw Error("No value stored under '$name'")
                when (stored) {
                    is Expr -> stored.eval()
                    is Double, is Boolean, is String -> stored
                    else -> throw Error("Unsupported value in variable '$name'")
                }
            }
            Symbol.REAL, Symbol.MINUS, Symbol.PLUS, Symbol.LPAREN, Symbol.GET -> {
                val expr = additive()
                expr.eval()
            }
            else -> throw Error("Invalid value type in set()")
        }

        if (currentToken.symbol != Symbol.RPAREN)
            throw Error("Expected ')' after value in set()")

        currentToken = lex.getToken()

        if ('.' in key) {
            val (objectName, propertyName) = key.split('.', limit = 2)
            val obj = vars[objectName]
            if (obj is Marker) {
                obj.props[propertyName] = value
                println("✔ set('$objectName.$propertyName') = $value (in props)")
                return
            }
        }

        vars[key] = value
        println("✔ set('$key') = $value")
    }


    private fun assignment() {
        if (currentToken.symbol == Symbol.VARIABLE) {
            val name = currentToken.lexeme
            currentToken = lex.getToken()
            if (currentToken.symbol == Symbol.ASSIGN) {
                currentToken = lex.getToken()
                assignment2(name)
                return
            } else {
                throw Error("Expected '=' after variable in assignment")
            }
        }
        throw Error("Expected variable at start of assignment")
    }


    private fun assignment2(name: String) {
        when (currentToken.symbol) {
            Symbol.POINT -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()
                    val x = additive()

                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()
                        val y = additive()
                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                            val point = Point(x, y)
                            vars[name] = point
                            return
                        } else {
                            throw Error("Expected ')' after point coordinates")
                        }
                    } else {
                        throw Error("Expected 'to' in point")
                    }
                } else {
                    throw Error("Expected '(' after 'point'")
                }
            }
            Symbol.STRING -> {
                val strValue = currentToken.lexeme
                currentToken = lex.getToken()
                vars[name] = strValue
            }
            else -> {
                val expr = additive()
                vars[name] = expr
                when (expr) {
                    is Expr.AreaExpr, is Expr.CountExpr -> {
                        println("✔ '$name' = ${expr.eval()}")
                    }
                }
            }
        }
    }




    private fun reassignment() {
        if(currentToken.symbol == Symbol.ASSIGN){
            currentToken = lex.getToken()
            reassignment2()
            return
        }
        throw Error("Invalid ReAssignment")
    }

    fun reassignment2() {
        when(currentToken.symbol){
            Symbol.LPAREN -> {
                currentToken = lex.getToken()
                additive()
                if (currentToken.symbol == Symbol.TO) {
                    currentToken = lex.getToken()
                    additive()
                    if (currentToken.symbol == Symbol.RPAREN) {
                        currentToken = lex.getToken()
                        return
                    }
                }
                throw Error("Invalid")
            }
            Symbol.KABEL, Symbol.PROIZVAJALCI, Symbol.SENZOR, Symbol.BATERIJA -> {
                block()
                return
            }
            Symbol.BEND, Symbol.LINE, Symbol.BOX, Symbol.CIRCLE, Symbol.MARKER -> {
                command()
                return
            }
            Symbol.STRING -> {
                currentToken = lex.getToken()
                return
            }
            else -> {
                additive()
                return
            }
        }
    }

    private fun constructs() {
        if (currentToken.symbol == Symbol.EOF || currentToken.symbol == Symbol.END) {
            return
        }
        constructTerminator()
        constructs()
    }

    private fun constructTerminator() {
        construct()
        if (currentToken.symbol == Symbol.SEMICOLON) {

            currentToken = lex.getToken()
        } else {


            throw Error("Missing construct terminator")
        }
    }


    private fun construct() {
        println("ENTERING construct with: ${currentToken.symbol}, lexeme='${currentToken.lexeme}'")
        when (currentToken.symbol) {
            Symbol.ZELENAMREZA -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.STRING) {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.BEGIN) {
                        currentToken = lex.getToken()
                        blocks()
                        if (currentToken.symbol == Symbol.END) {
                            currentToken = lex.getToken()
                        } else {
                            throw Error("Expected 'END' after city block")
                        }
                    } else {
                        throw Error("Expected 'BEGIN' after city name")
                    }
                } else {
                    throw Error("Expected city name (string)")
                }
            }

            Symbol.LET -> {
                currentToken = lex.getToken()
                assignment()
                if (currentToken.symbol == Symbol.SEMICOLON) {
                    currentToken = lex.getToken()
                } else {
                    throw Error("Missing ';' after let assignment")
                }
            }

            Symbol.LINE -> {
                println("ENTERING construct with: LINE, lexeme='${currentToken.lexeme}'")
                command()

            }
            Symbol.BOX -> {
                println("ENTERING construct with: BOX, lexeme='${currentToken.lexeme}'")
                command()
            }
            Symbol.CIRCLE -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()
                    val center = if (currentToken.symbol == Symbol.VARIABLE) {
                        val name = currentToken.lexeme
                        currentToken = lex.getToken()
                        name
                    } else {
                        throw Error("Expected variable as center point in CIRCLE")
                    }
                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()
                        val radius = additive()
                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                            commands.add(Circle(center, radius))
                            return
                        }
                    }
                }
                throw Error("Invalid CIRCLE syntax")
            }
            Symbol.MARKER -> {
                currentToken = lex.getToken()

                val pointName = if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.VARIABLE) {
                        val name = currentToken.lexeme
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                            name
                        } else {
                            throw Error("Expected ')' after variable in marker(...)")
                        }
                    } else {
                        throw Error("Expected variable inside marker(...)")
                    }
                } else {
                    throw Error("Expected '(' after 'marker'")
                }


                val props = mutableMapOf<String, Any>()

                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()

                    while (true) {
                        when (currentToken.symbol) {
                            Symbol.COLOR -> {
                                currentToken = lex.getToken()
                                expectAssign()
                                props["color"] = currentToken.lexeme
                                currentToken = lex.getToken()
                            }
                            Symbol.LABEL -> {
                                currentToken = lex.getToken()
                                expectAssign()
                                if (currentToken.symbol != Symbol.STRING) {
                                    throw Error("Expected string after label=")
                                }
                                props["label"] = currentToken.lexeme
                                currentToken = lex.getToken()
                            }
                            Symbol.VALUE -> {
                                currentToken = lex.getToken()
                                expectAssign()
                                props["value"] = primary().eval()
                            }
                            else -> break
                        }

                        if (currentToken.symbol == Symbol.TO) {
                            currentToken = lex.getToken()
                        } else {
                            break
                        }
                    }

                    if (currentToken.symbol == Symbol.RPAREN) {
                        currentToken = lex.getToken()
                    } else {
                        throw Error("Expected ')' after marker properties")
                    }
                }

                commands.add(Marker(pointName, props))
                return
            }
            Symbol.KABEL -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.STRING) {
                    val kabelName = currentToken.lexeme
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.BEGIN) {
                        currentToken = lex.getToken()
                        kabelBody(kabelName)
                        if (currentToken.symbol == Symbol.END) {
                            currentToken = lex.getToken()
                        } else throw Error("Expected END")

                        if (currentToken.symbol == Symbol.SEMICOLON) {
                            currentToken = lex.getToken()
                        } else {
                            throw Error("Missing ';' after kabel block")
                        }
                    } else throw Error("Expected BEGIN")
                } else throw Error("Expected STRING")
            }



            Symbol.CONNECT -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()

                    val from = if (currentToken.symbol == Symbol.STRING || currentToken.symbol == Symbol.VARIABLE) {
                        val name = currentToken.lexeme
                        currentToken = lex.getToken()
                        name
                    } else throw Error("Expected first entity name in connect(...)")

                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()
                    } else throw Error("Expected ',' between connect arguments")

                    val to = if (currentToken.symbol == Symbol.STRING || currentToken.symbol == Symbol.VARIABLE) {
                        val name = currentToken.lexeme
                        currentToken = lex.getToken()
                        name
                    } else throw Error("Expected second entity name in connect(...)")

                    if (currentToken.symbol == Symbol.RPAREN) {
                        currentToken = lex.getToken()
                        commands.add(Connect(from, to))
                        return
                    } else throw Error("Expected ')' after connect(...)")
                } else throw Error("Expected '(' after 'connect'")
            }


            Symbol.VARIABLE -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.SEMICOLON) {
                    currentToken = lex.getToken()
                    return
                }
                reassignment()
                if (currentToken.symbol == Symbol.SEMICOLON) {
                    currentToken = lex.getToken()
                } else {
                    throw Error("Missing construct terminator after reassignment")
                }
            }


            else -> throw Error("Invalid construct")
        }
    }

    private fun blocks() {
        if (currentToken.symbol == Symbol.EOF || currentToken.symbol == Symbol.END) {
            return
        }
        blockTerminator()
        blocks()
    }
    private fun blockTerminator() {
        block()

        if (currentToken.symbol == Symbol.SEMICOLON) {
            currentToken = lex.getToken()
        } else {
            throw Error("Missing 'term' after block, got: ${currentToken.symbol}")
        }
    }



    private fun block() {
        when (currentToken.symbol) {
            Symbol.KABEL -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.STRING) {
                    val kabelName = currentToken.lexeme
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.BEGIN) {
                        currentToken = lex.getToken()
                        kabelBody(kabelName)
                        if (currentToken.symbol == Symbol.END) {
                            currentToken = lex.getToken()
                        } else {
                            throw Error("Expected END after kabel block")
                        }
                    } else {
                        throw Error("Expected BEGIN after kabel name")
                    }
                } else {
                    throw Error("Expected STRING after KABEL")
                }
            }

            Symbol.PROIZVAJALCI -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.STRING) {
                    val name = currentToken.lexeme
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.BEGIN) {
                        currentToken = lex.getToken()
                        val beforeSize = commands.size
                        commands()
                        if (currentToken.symbol == Symbol.END) {
                            currentToken = lex.getToken()
                            val newCommands = commands.drop(beforeSize)
                            if (newCommands.isNotEmpty()) {
                                vars[name] = newCommands.last()

                            }
                        } else {
                            throw Error("Expected END after proizvajalci block")
                        }
                    } else {
                        throw Error("Expected BEGIN after proizvajalci name")
                    }
                } else {
                    throw Error("Expected STRING after PROIZVAJALCI")
                }
            }

            Symbol.SENZOR -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.STRING) {
                    val name = currentToken.lexeme
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.BEGIN) {
                        currentToken = lex.getToken()

                        val beforeSize = commands.size
                        commands()

                        if (currentToken.symbol == Symbol.END) {
                            currentToken = lex.getToken()

                            val newCommands = commands.drop(beforeSize)
                            if (newCommands.isNotEmpty()) {
                                vars[name] = newCommands.last()
                                println("✔ senzor '$name' mapped to ${newCommands.last().javaClass.simpleName}")
                            }

                        } else {
                            throw Error("Expected END after senzor block")
                        }
                    } else {
                        throw Error("Expected BEGIN after senzor name")
                    }
                } else {
                    throw Error("Expected STRING after SENZOR")
                }
            }


            Symbol.BATERIJA -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.STRING) {
                    val name = currentToken.lexeme
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.BEGIN) {
                        currentToken = lex.getToken()
                        val beforeSize = commands.size
                        commands()
                        if (currentToken.symbol == Symbol.END) {
                            currentToken = lex.getToken()
                            val newCommands = commands.drop(beforeSize)
                            if (newCommands.isNotEmpty()) {
                                vars[name] = newCommands.last()

                            }
                        } else {
                            throw Error("Expected END after baterija block")
                        }
                    } else {
                        throw Error("Expected BEGIN after baterija name")
                    }
                } else {
                    throw Error("Expected STRING after BATERIJA")
                }
            }



            Symbol.SET -> {
                currentToken = lex.getToken()
                set()
            }

            Symbol.LET -> {
                currentToken = lex.getToken()
                assignment()
            }
            Symbol.GET -> {
                currentToken = lex.getToken()
                get()
            }

            Symbol.VARIABLE -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.SEMICOLON) {
                    currentToken = lex.getToken()
                } else {
                    reassignment()
                }
            }

            Symbol.BOX, Symbol.CONNECT, Symbol.MARKER, Symbol.CIRCLE -> {
                command()
            }

            Symbol.CONNECT -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()

                    entity()
                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()
                        entity()
                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                        } else {
                            throw Error("Expected ')' after connect arguments")
                        }
                    } else {
                        throw Error("Expected ',' between connect arguments")
                    }
                } else {
                    throw Error("Expected '(' after 'connect'")
                }
            }

            else -> {
                println("tu je block error: ${currentToken.symbol}, lexeme='${currentToken.lexeme}'")
                throw Error("Invalid block start")
            }
        }
    }


    private fun parsePointExpr(): Point {
        if (currentToken.symbol != Symbol.LPAREN) {
            throw Error("Expected '(' to start point expression")
        }
        currentToken = lex.getToken()
        val x = additive()

        if (currentToken.symbol != Symbol.TO) {
            throw Error("Expected ',' between coordinates")
        }
        currentToken = lex.getToken()
        val y = additive()

        if (currentToken.symbol != Symbol.RPAREN) {
            throw Error("Expected ')' to end point expression")
        }
        currentToken = lex.getToken()
        return Point(x, y)
    }




    private fun kabelBody(name: String) {
        val segments = mutableListOf<KabelSegment>()
        kabelLines(segments)
        val simpleSegments = segments.map { it.from to it.to }
        commands.add(Kabel(name, simpleSegments))
    }


    private fun kabelLines(segments: MutableList<KabelSegment>) {
        while (currentToken.symbol != Symbol.EOF && currentToken.symbol != Symbol.END) {
            kabelLine(segments)
        }
    }

    private fun kabelLine(segments: MutableList<KabelSegment>) {
        when (currentToken.symbol) {
            Symbol.BEND -> {
                currentToken = lex.getToken()
                if (currentToken.symbol != Symbol.LPAREN) throw Error("Expected '(' after 'bend'")
                currentToken = lex.getToken()
                val from = parsePointExpr()

                if (currentToken.symbol != Symbol.TO) throw Error("Expected ',' between points")
                currentToken = lex.getToken()
                val to = parsePointExpr()

                if (currentToken.symbol != Symbol.TO) throw Error("Expected ',' before bend amount")
                currentToken = lex.getToken()
                val radiusExpr = additive()
                val radius = radiusExpr.eval()

                if (currentToken.symbol != Symbol.RPAREN) throw Error("Expected ')' after bend parameters")
                currentToken = lex.getToken()

                segments.add(KabelSegment(from, to, radius))
            }

            Symbol.LINE -> {
                currentToken = lex.getToken()
                if (currentToken.symbol != Symbol.LPAREN) throw Error("Expected '(' after 'line'")
                currentToken = lex.getToken()

                val fromName = if (currentToken.symbol == Symbol.VARIABLE) {
                    val name = currentToken.lexeme
                    currentToken = lex.getToken()
                    name
                } else {
                    throw Error("Expected variable as first point in line()")
                }

                if (currentToken.symbol != Symbol.TO) throw Error("Expected ',' between line points")
                currentToken = lex.getToken()

                val toName = if (currentToken.symbol == Symbol.VARIABLE) {
                    val name = currentToken.lexeme
                    currentToken = lex.getToken()
                    name
                } else {
                    throw Error("Expected variable as second point in line()")
                }

                if (currentToken.symbol != Symbol.RPAREN) throw Error("Expected ')' after line points")
                currentToken = lex.getToken()

                val fromPoint = vars[fromName] as? Point ?: throw Error("Point '$fromName' not defined or invalid")
                val toPoint = vars[toName] as? Point ?: throw Error("Point '$toName' not defined or invalid")

                segments.add(KabelSegment(fromPoint, toPoint, 0.0))
            }


            else -> {
                val from = parsePointExpr()
                kabelCont(from, segments)
            }
        }

        if (currentToken.symbol == Symbol.SEMICOLON) {
            currentToken = lex.getToken()
        } else {
            throw Error("Missing terminator at ${currentToken.row}:${currentToken.column}")
        }
    }


    private fun kabelCont(fromStart: Point, segments: MutableList<KabelSegment>) {
        var from = fromStart
        while (currentToken.symbol == Symbol.LINK) {
            currentToken = lex.getToken()

            var bendRadius: Double? = null
            if (currentToken.symbol == Symbol.BEND) {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.REAL || currentToken.symbol == Symbol.VARIABLE) {
                    val radiusExpr = additive()
                    bendRadius = radiusExpr.eval()
                }
            }

            val to = parsePointExpr()
            segments.add(KabelSegment(from, to, bendRadius))
            from = to
        }
    }









    private fun commands() {
        if (currentToken.symbol == Symbol.EOF || currentToken.symbol == Symbol.END) {
            return
        }
        commandTerminator()
        commands()
    }

    private fun commandTerminator() {
        command()
        if (currentToken.symbol == Symbol.SEMICOLON) {
            currentToken = lex.getToken()
        } else {
            throw Error("Missing terminator")
        }
    }

    private fun command() {
        when(currentToken.symbol){
            Symbol.BEND -> {
                currentToken = lex.getToken()
                if(currentToken.symbol == Symbol.LPAREN){
                    currentToken = lex.getToken()
                    if(currentToken.symbol == Symbol.VARIABLE){
                        currentToken = lex.getToken()
                    } else {
                        point()
                    }
                    if(currentToken.symbol == Symbol.TO){
                        currentToken = lex.getToken()
                        if(currentToken.symbol == Symbol.VARIABLE){
                            currentToken = lex.getToken()
                        } else {
                            point()
                        }
                        if(currentToken.symbol == Symbol.TO){
                            currentToken = lex.getToken()
                            additive()
                            if(currentToken.symbol == Symbol.RPAREN){
                                currentToken = lex.getToken()
                                return
                            }
                        }
                    }
                }
                throw Error("Invalid")
            }
            Symbol.LINE -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()

                    val from = if (currentToken.symbol == Symbol.VARIABLE) {
                        val name = currentToken.lexeme
                        currentToken = lex.getToken()
                        name
                    } else {
                        throw Error("Expected variable as first point")
                    }

                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()

                        val to = if (currentToken.symbol == Symbol.VARIABLE) {
                            val name = currentToken.lexeme
                            currentToken = lex.getToken()
                            name
                        } else {
                            throw Error("Expected variable as second point")
                        }

                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                            commands.add(Line(from, to))
                            return
                        }
                    }
                }
                throw Error("Invalid line syntax")
            }


            Symbol.BOX -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()

                    val from: String = when (currentToken.symbol) {
                        Symbol.LPAREN -> {
                            val point = parsePointExpr()
                            // Ustvari začasno ime za literalno točko
                            val tempName = "__point__${point.x.eval()}_${point.y.eval()}"
                            vars[tempName] = point
                            tempName
                        }
                        Symbol.VARIABLE -> {
                            val name = currentToken.lexeme
                            currentToken = lex.getToken()
                            name
                        }
                        else -> throw Error("Expected variable or point as first argument in BOX")
                    }

                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()
                        val to: String = when (currentToken.symbol) {
                            Symbol.LPAREN -> {
                                val point = parsePointExpr()
                                val tempName = "__point__${point.x.eval()}_${point.y.eval()}"
                                vars[tempName] = point
                                tempName
                            }
                            Symbol.VARIABLE -> {
                                val name = currentToken.lexeme
                                currentToken = lex.getToken()
                                name
                            }
                            else -> throw Error("Expected variable or point as second argument in BOX")
                        }

                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                            commands.add(Box(from, to))
                            return
                        } else {
                            throw Error("Expected ')' after BOX arguments")
                        }
                    } else {
                        throw Error("Expected ',' between BOX arguments")
                    }
                } else {
                    throw Error("Invalid BOX syntax")
                }
            }



            Symbol.CIRCLE -> {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()
                    val center = if (currentToken.symbol == Symbol.VARIABLE) {
                        val name = currentToken.lexeme
                        currentToken = lex.getToken()
                        name
                    } else {
                        throw Error("Expected variable as center point in CIRCLE")
                    }
                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()
                        val radius = additive()
                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                            // Tukaj dodaš Circle v commands
                            commands.add(Circle(center, radius))
                            return
                        } else {
                            throw Error("Expected ')' after circle radius")
                        }
                    } else {
                        throw Error("Expected ',' between center and radius in CIRCLE")
                    }
                } else {
                    throw Error("Expected '(' after CIRCLE")
                }
            }
            Symbol.GET -> {
                currentToken = lex.getToken()
                val expr = get()
                println("✔ get(...) = ${expr.eval()}")
                return
            }

            Symbol.MARKER -> {
                currentToken = lex.getToken()

                val pointRef = when {
                    // Handle variable case (marker(p))
                    currentToken.symbol == Symbol.VARIABLE -> {
                        val name = currentToken.lexeme
                        currentToken = lex.getToken()
                        name
                    }
                    // Handle point literal case (marker(1,2))
                    currentToken.symbol == Symbol.LPAREN -> {
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.VARIABLE) {
                            // Handle marker((p)) case
                            val name = currentToken.lexeme
                            currentToken = lex.getToken()
                            if (currentToken.symbol != Symbol.RPAREN) {
                                throw Error("Expected ')' after variable")
                            }
                            currentToken = lex.getToken()
                            name
                        } else {
                            // Handle literal coordinates (marker((1,2)))
                            val point = parsePointExpr()
                            val tempName = "__point__${point.x.eval()}_${point.y.eval()}"
                            vars[tempName] = point
                            tempName
                        }
                    }
                    else -> throw Error("Expected variable or point in marker")
                }

                // Handle properties if they exist
                val props = mutableMapOf<String, Any>()
                if (currentToken.symbol == Symbol.LPAREN) {
                    currentToken = lex.getToken()
                    while (true) {
                        when (currentToken.symbol) {
                            Symbol.COLOR -> {
                                currentToken = lex.getToken()
                                expectAssign()
                                props["color"] = currentToken.lexeme
                                currentToken = lex.getToken()
                            }
                            Symbol.LABEL -> {
                                currentToken = lex.getToken()
                                expectAssign()
                                if (currentToken.symbol != Symbol.STRING) {
                                    throw Error("Expected string after label=")
                                }
                                props["label"] = currentToken.lexeme
                                currentToken = lex.getToken()
                            }
                            Symbol.VALUE -> {
                                currentToken = lex.getToken()
                                expectAssign()
                                props["value"] = primary().eval()
                            }
                            else -> break
                        }
                        if (currentToken.symbol == Symbol.TO) {
                            currentToken = lex.getToken()
                        } else {
                            break
                        }
                    }
                    if (currentToken.symbol != Symbol.RPAREN) {
                        throw Error("Expected ')' after marker properties")
                    }
                    currentToken = lex.getToken()
                }

                commands.add(Marker(pointRef, props))
                return
            }



            Symbol.KABEL -> {
                currentToken = lex.getToken()

                val kabelName = if (currentToken.symbol == Symbol.STRING) {
                    val name = currentToken.lexeme
                    currentToken = lex.getToken()
                    name
                } else {
                    throw Error("Expected string after 'kabel'")
                }

                if (currentToken.symbol != Symbol.BEGIN) {
                    throw Error("Expected '{' after kabel name")
                }
                currentToken = lex.getToken()

                val segments = mutableListOf<Pair<Point, Point>>()

                while (currentToken.symbol != Symbol.BEGIN) {
                    if (currentToken.symbol == Symbol.BEND) {
                        currentToken = lex.getToken()
                        if (currentToken.symbol != Symbol.LPAREN) throw Error("Expected '(' after 'bend'")
                        currentToken = lex.getToken()
                        val from = parsePointExpr()
                        if (currentToken.symbol != Symbol.TO) throw Error("Expected ',' between points")
                        currentToken = lex.getToken()
                        val to = parsePointExpr()
                        if (currentToken.symbol != Symbol.TO) throw Error("Expected ',' before radius")
                        currentToken = lex.getToken()
                        val bendAmount = additive() // lahko tudi ignoriraš za zdaj
                        if (currentToken.symbol != Symbol.RPAREN) throw Error("Expected ')' after bend params")
                        currentToken = lex.getToken()

                        segments.add(from to to)
                    } else {
                        throw Error("Expected 'bend' in kabel body")
                    }

                    if (currentToken.symbol == Symbol.SEMICOLON) {
                        currentToken = lex.getToken()
                    } else {
                        throw Error("Missing ';' after bend")
                    }
                }

                // zapremo zaklepaj
                currentToken = lex.getToken()

                // dodamo eval objekt
                commands.add(Kabel(kabelName, segments))
                return
            }

            Symbol.SET -> {
                currentToken = lex.getToken()
                set()
                return
            }
            Symbol.LET ->{
                currentToken = lex.getToken()
                assignment()
                return
            }
            Symbol.VARIABLE -> {
                currentToken = lex.getToken()
                if(currentToken.symbol == Symbol.SEMICOLON){
                    return
                }
                reassignment()
                return
            }
            Symbol.CONNECT -> {
                currentToken = lex.getToken()

                if (currentToken.symbol != Symbol.LPAREN)
                    throw Error("Expected '(' after 'connect'")

                currentToken = lex.getToken()

                val from = if (currentToken.symbol == Symbol.STRING || currentToken.symbol == Symbol.VARIABLE) {
                    val name = currentToken.lexeme
                    currentToken = lex.getToken()
                    name
                } else throw Error("Expected from-entity in connect(...)")

                if (currentToken.symbol != Symbol.TO)
                    throw Error("Expected TO between connect args")

                currentToken = lex.getToken()

                val to = if (currentToken.symbol == Symbol.STRING || currentToken.symbol == Symbol.VARIABLE) {
                    val name = currentToken.lexeme
                    currentToken = lex.getToken()
                    name
                } else throw Error("Expected to-entity in connect(...)")

                if (currentToken.symbol != Symbol.RPAREN)
                    throw Error("Expected ')' after connect(...)")

                currentToken = lex.getToken()

                var bendAngle: Double? = null
                if (currentToken.symbol == Symbol.BEND) {
                    currentToken = lex.getToken()
                    val expr = additive()
                    bendAngle = expr.eval()
                }

                commands.add(Connect(from, to, bendAngle))
            }


            else -> throw Error("Invalid")
        }
    }



}
