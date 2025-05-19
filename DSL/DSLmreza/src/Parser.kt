class Parser(
    private val lex: Scanner,
    private var currentToken: Token = lex.getToken()
) {

    fun parse() {
        try {
            point()
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
        while (currentToken.symbol == Symbol.PLUS || currentToken.symbol == Symbol.MINUS) {
            match(currentToken.symbol)
            multiplicative()
        }
    }

    private fun multiplicative() {
        unary()
        while (currentToken.symbol == Symbol.TIMES ||
            currentToken.symbol == Symbol.DIVIDES) {
            match(currentToken.symbol)
            unary()
        }
    }

    private fun unary() {
        if (currentToken.symbol == Symbol.PLUS || currentToken.symbol == Symbol.MINUS) {
            match(currentToken.symbol)
        }
        primary()
    }

    private fun primary() {
        when (currentToken.symbol) {
            Symbol.REAL, Symbol.VARIABLE, Symbol.TRUE, Symbol.FALSE -> {
                match(currentToken.symbol)
            }
            Symbol.LPAREN -> {
                match(Symbol.LPAREN)
                additive()
                match(Symbol.RPAREN)
            }
            Symbol.GET -> {
                match(Symbol.GET)
                parseGet()
            }
            else -> error("Invalid primary at ${currentToken.row}:${currentToken.column}")
        }
    }

    private fun parseGet() {
        match(Symbol.LPAREN)
        if (currentToken.symbol == Symbol.STRING) {
            match(Symbol.STRING)
            match(Symbol.RPAREN)
        } else {
            error("Expected string in get()")
        }
    }

    private fun match(expected: Symbol) {
        if (currentToken.symbol == expected) {
            currentToken = lex.getToken()
        } else {
            error("Expected $expected but got ${currentToken.symbol} at ${currentToken.row}:${currentToken.column}")
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
                    return  // Vse ok, vrni iz funkcije
                }
            }
        }
        throw Error("Invalid point syntax at ${currentToken.row}:${currentToken.column}")
    }




}
