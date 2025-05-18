import java.io.InputStream




class Scanner(inputStream: InputStream) {

    private val reader = inputStream.bufferedReader()
    private var peekedChar: Int? = null
    private var row = 1
    private var column = 0

    private fun readChar(): Int {
        val char = peekedChar ?: reader.read()
        peekedChar = null
        if (char == '\n'.code) {
            row++
            column = 0
        } else {
            column++
        }
        return char
    }

    private fun peekChar(): Int {
        if (peekedChar == null) {
            peekedChar = reader.read()
        }
        return peekedChar!!
    }

    fun nextToken(): Token {
        skipWhitespace()

        val startColumn = column
        val startRow = row
        val c = peekChar()
        if (c == -1) return Token(SymbolType.EOF, "", startRow, startColumn)

        val ch = c.toChar()

        // === IDENTIFIKATOR (VARIABLE) ali KLJUČNA BESEDA ===
        if (ch.isLetter() || ch == '_') {
            val buffer = StringBuilder()
            while (peekChar().toChar().isLetterOrDigit() || peekChar().toChar() == '_') {
                buffer.append(readChar().toChar())
            }
            val lexeme = buffer.toString()
            val keyword = when (lexeme) {
                "true" -> SymbolType.TRUE
                "false" -> SymbolType.FALSE
                "set" -> SymbolType.SET
                "bend" -> SymbolType.BEND
                "circle" -> SymbolType.CIRCLE
                "connect" -> SymbolType.CONNECT
                "box" -> SymbolType.BOX

                "get" -> SymbolType.GET
                "marker" -> SymbolType.MARKER
                "line" -> SymbolType.LINE
                "area" -> SymbolType.AREA
                "count" -> SymbolType.COUNT
                "zelenamreza" -> SymbolType.ZELENAMREZA
                "link" -> SymbolType.LINK
                else -> SymbolType.VARIABLE
            }
            return Token(keyword, lexeme, startRow, startColumn)
        }

        // === NIZ (STRING) ===
        if (ch == '"') {
            readChar() // preberi začetni narekovaj
            val buffer = StringBuilder()
            while (true) {
                val c = peekChar()
                if (c == -1 || c.toChar() == '"') {
                    readChar() // preberi končni narekovaj ali EOF
                    break
                }
                buffer.append(readChar().toChar())
            }
            return Token(SymbolType.STRING, buffer.toString(), startRow, startColumn)
        }

        // === REALNO ŠTEVILO ===
        if (ch.isDigit()) {
            val buffer = StringBuilder()
            var hasDot = false
            while (true) {
                val nextChar = peekChar().toChar()
                if (nextChar.isDigit()) {
                    buffer.append(readChar().toChar())
                } else if (nextChar == '.' && !hasDot) {
                    hasDot = true
                    buffer.append(readChar().toChar())
                } else {
                    break
                }
            }
            return Token(SymbolType.REAL, buffer.toString(), startRow, startColumn)
        }

        // === ENOZNAČNI SIMBOLI ===
        when (readChar().toChar()) {
            '+' -> return Token(SymbolType.PLUS, "+", startRow, startColumn)
            '-' -> {
                // Preverimo ali je to sladkorček --
                if (peekChar().toChar() == '-') {
                    readChar()
                    return Token(SymbolType.LINK, "--", startRow, startColumn)
                }
                return Token(SymbolType.MINUS, "-", startRow, startColumn)
            }
            '*' -> return Token(SymbolType.TIMES, "*", startRow, startColumn)
            '/' -> return Token(SymbolType.DIVIDES, "/", startRow, startColumn)
            ':' -> return Token(SymbolType.COLON, ":", startRow, startColumn)
            '=' -> return Token(SymbolType.ASSIGN, "=", startRow, startColumn)
            '(' -> return Token(SymbolType.LPAREN, "(", startRow, startColumn)
            ')' -> return Token(SymbolType.RPAREN, ")", startRow, startColumn)
            '{' -> return Token(SymbolType.LBRACE, "{", startRow, startColumn)
            '}' -> return Token(SymbolType.RBRACE, "}", startRow, startColumn)
            '.' -> return Token(SymbolType.POINT, ".", startRow, startColumn)
            ';' -> return Token(SymbolType.SEMICOLON, ";", startRow, startColumn)
        }

        return Token(SymbolType.VARIABLE, readChar().toChar().toString(), startRow, startColumn)
    }


    private fun skipWhitespace() {
        while (true) {
            val c = peekChar()
            if (c == -1) break
            if (c.toChar().isWhitespace()) readChar() else break
        }
    }


}
