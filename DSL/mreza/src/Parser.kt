    class Parser(
        private val lex: Scanner,
        private var currentToken: Token = lex.getToken()
    ) {
    
        fun parse() {
            try {
                constructs() //constructs
                if (currentToken.symbol == Symbol.EOF) {
                    println("accept")
                } else {
                    println("reject")
                }
            } catch (e: Exception) {
                println("reject")
            }
        }
    
    
        private fun additive() {
            multiplicative()
            additive2()
        }
    
        private fun additive2() {
            if (currentToken.symbol == Symbol.PLUS || currentToken.symbol == Symbol.MINUS) {
                currentToken = lex.getToken()
                multiplicative()
                additive2()
            }
        }
    
        private fun multiplicative() {
            unary()
            multiplicative2()
        }
    
        private fun multiplicative2() {
            if (currentToken.symbol == Symbol.TIMES || currentToken.symbol == Symbol.DIVIDES) {
                currentToken = lex.getToken()
                unary()
                multiplicative2()
            }
        }
    
        private fun unary() {
            if (currentToken.symbol == Symbol.PLUS || currentToken.symbol == Symbol.MINUS) {
                currentToken = lex.getToken()
            }
            primary()
        }
    
        private fun get() {
            if (currentToken.symbol == Symbol.LPAREN) {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.STRING) {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.RPAREN) {
                        currentToken = lex.getToken()
                        return
                    }
                }
            }
            throw Error("Invalid Get")
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
    
    
    
        private fun primary() {
            when (currentToken.symbol) {
                Symbol.REAL, Symbol.VARIABLE,
                Symbol.TRUE, Symbol.FALSE -> {
                    currentToken = lex.getToken()
                }
                Symbol.LPAREN -> {
                    currentToken = lex.getToken()
                    additive()
                    if (currentToken.symbol == Symbol.RPAREN) {
                        currentToken = lex.getToken()
                    } else {
                        throw Error("Expected ')'")
                    }
                }
                Symbol.GET -> {
                    currentToken = lex.getToken()
                    get()
                }
                Symbol.COUNT -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.LPAREN) {
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.STRING) {
                            currentToken = lex.getToken()
                            if (currentToken.symbol == Symbol.RPAREN) {
                                currentToken = lex.getToken()
    
                                return
                            }
                        }
                        throw Error("Invalid count() syntax at ${currentToken.row}:${currentToken.column}")
                    }
                    throw Error("Expected '(' after 'count'")
                }
    
                Symbol.AREA -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.LPAREN) {
                        currentToken = lex.getToken()
                        areaArgs()
                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                            return
                        } else {
                            throw Error("Expected ')' after area args")
                        }
                    } else {
                        throw Error("Expected '(' after 'area'")
                    }
                }
                else -> throw Error("Invalid primary")
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
    
        private fun point() {
            if (currentToken.symbol == Symbol.LPAREN) {
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
            }
            throw Error("Invalid point syntax at ${currentToken.row}:${currentToken.column}")
        }
    
        private fun set() {
            if (currentToken.symbol == Symbol.LPAREN) {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.STRING) {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.TO) {
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.STRING ||
                            currentToken.symbol == Symbol.TRUE ||
                            currentToken.symbol == Symbol.FALSE) {
                            currentToken = lex.getToken()
                            if (currentToken.symbol == Symbol.RPAREN) {
                                currentToken = lex.getToken()
                                return
                            } else {
                                throw Error("Expected ')' after value")
                            }
                        }
                        else if (currentToken.symbol == Symbol.VARIABLE || currentToken.symbol == Symbol.REAL ||
                            currentToken.symbol == Symbol.MINUS || currentToken.symbol == Symbol.PLUS ||
                            currentToken.symbol == Symbol.LPAREN || currentToken.symbol == Symbol.GET) {
                            additive()
                            if (currentToken.symbol == Symbol.RPAREN) {
                                currentToken = lex.getToken()
                                return
                            } else {
                                throw Error("Expected ')' after expression")
                            }
                        } else {
                            throw Error("Invalid value after 'to'")
                        }
                    } else {
                        throw Error("Expected 'to' after property name")
                    }
                } else {
                    throw Error("Expected string as property name")
                }
            }
            throw Error("Expected '(' at beginning of set()")
        }
    
        private fun assignment() {
            if (currentToken.symbol == Symbol.VARIABLE) {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.ASSIGN) {
                    currentToken = lex.getToken()
                    assignment2()
                    return
                } else {
                    throw Error("Expected '=' after variable in assignment")
                }
            }
            throw Error("Expected variable at start of assignment")
        }
    
        private fun assignment2() {
            when (currentToken.symbol) {
                Symbol.POINT -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.LPAREN) {
                        currentToken = lex.getToken()
                        additive()
                        if (currentToken.symbol == Symbol.TO) {
                            currentToken = lex.getToken()
                            additive()
                            if (currentToken.symbol == Symbol.RPAREN) {
                                currentToken = lex.getToken()
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
    
                Symbol.KABEL, Symbol.PROIZVAJALCI, Symbol.SENZOR, Symbol.BATERIJA -> {
                    block()
                    return
                }
    
                Symbol.BEND, Symbol.LINE, Symbol.BOX, Symbol.CIRCLE, Symbol.MARKER, Symbol.COUNT, Symbol.AREA, Symbol.CONNECT -> {
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
                        throw Error("Missing construct terminator after let assignment")
                    }
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
                throw Error("Missing 'term' after block")
            }
        }
    
        private fun block() {
            when (currentToken.symbol) {
                Symbol.KABEL -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.STRING) {
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.BEGIN) {
                            currentToken = lex.getToken()

                            kabelBody()  // ← tukaj kličemo pravilen razpoznavalnik za telo kabla

                            if (currentToken.symbol == Symbol.END) {
                                currentToken = lex.getToken()
                            } else throw Error("Expected END")
                        } else throw Error("Expected BEGIN")
                    } else throw Error("Expected STRING")
                }
                Symbol.PROIZVAJALCI -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.STRING) {
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.BEGIN) {
                            currentToken = lex.getToken()
                            commands()
                            if (currentToken.symbol == Symbol.END) {
                                currentToken = lex.getToken()
                            } else throw Error("Expected END")
                        } else throw Error("Expected BEGIN")
                    } else throw Error("Expected STRING")
                }
                Symbol.SENZOR -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.STRING) {
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.BEGIN) {
                            currentToken = lex.getToken()
                            commands()
                            if (currentToken.symbol == Symbol.END) {
                                currentToken = lex.getToken()
                            } else throw Error("Expected END")
                        } else throw Error("Expected BEGIN")
                    } else throw Error("Expected STRING")
                }
                Symbol.BATERIJA -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.STRING) {
                        currentToken = lex.getToken()
                        if (currentToken.symbol == Symbol.BEGIN) {
                            currentToken = lex.getToken()
                            commands()
                            if (currentToken.symbol == Symbol.END) {
                                currentToken = lex.getToken()
                            } else throw Error("Expected END")
                        } else throw Error("Expected BEGIN")
                    } else throw Error("Expected STRING")
                }
                Symbol.SET -> {
                    currentToken = lex.getToken()
                    set()
                }
                Symbol.LET -> {
                    currentToken = lex.getToken()
                    assignment()
                }
                Symbol.VARIABLE -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.SEMICOLON) {
                        currentToken = lex.getToken()
                    } else {
                        reassignment()
                    }
                }
    
    
                else -> throw Error("Invalid block start")
            }
        }
        private fun kabelBody(){
            if (currentToken.symbol == Symbol.BEND || currentToken.symbol == Symbol.LINE || currentToken.symbol == Symbol.CONNECT ) {
                commands()
            }else{
                kabelLines()
            }
    
        }
        private fun kabelLines() {
            if (currentToken.symbol == Symbol.EOF || currentToken.symbol == Symbol.END) {
                return
            }
            kabelLine()
            kabelLines()
        }
    
        private fun kabelLine(){
            point()
            kabelCont()
            if (currentToken.symbol == Symbol.SEMICOLON) {
                currentToken = lex.getToken()
            } else {
                throw Error("Missing terminator at ${currentToken.row}:${currentToken.column}")
            }
        }
    
        private fun kabelCont() {
            if (currentToken.symbol == Symbol.LINK) {
                currentToken = lex.getToken()
                if (currentToken.symbol == Symbol.BEND) {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.REAL || currentToken.symbol == Symbol.VARIABLE) {
                        currentToken = lex.getToken()
                    }
                }
                point()
                kabelCont()
            } else {
                return
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
                            if(currentToken.symbol == Symbol.RPAREN){
                                currentToken = lex.getToken()
                                return
                            }
                        }
                    }
                    throw Error("Invalid")
                }
                Symbol.BOX -> {
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
                            if(currentToken.symbol == Symbol.RPAREN){
                                currentToken = lex.getToken()
                                return
                            }
                        }
                    }
                    throw Error("Invalid")
                }
                Symbol.CIRCLE -> {
                    currentToken = lex.getToken()
                    if (currentToken.symbol == Symbol.LPAREN) {
                        currentToken = lex.getToken()
                        if(currentToken.symbol == Symbol.VARIABLE){
                            currentToken = lex.getToken()
                        } else {
                            point()
                        }
                        if (currentToken.symbol == Symbol.TO) {
                            currentToken = lex.getToken()
                            additive()
                            if (currentToken.symbol == Symbol.RPAREN) {
                                currentToken = lex.getToken()
                                return
                            }
                        }
                    }
                    throw Error("Invalid")
                }
                Symbol.MARKER -> {
                    currentToken = lex.getToken()
                    if(currentToken.symbol == Symbol.VARIABLE){
                        currentToken = lex.getToken()
                    } else {
                        point()
                    }
                    if (currentToken.symbol == Symbol.LPAREN) {
                        currentToken = lex.getToken()
                        properties()
                        if (currentToken.symbol == Symbol.RPAREN) {
                            currentToken = lex.getToken()
                        } else {
                            throw Error("Expected ')' after properties")
                        }
                    }
    
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
                    if (currentToken.symbol == Symbol.LPAREN) {
                        currentToken = lex.getToken()

                        entity()
                        if (currentToken.symbol == Symbol.TO) {
                            currentToken = lex.getToken()
                            entity()
                            if (currentToken.symbol == Symbol.RPAREN) {
                                currentToken = lex.getToken()
                                return
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

                else -> throw Error("Invalid")
            }
        }
    
    
    
    }
