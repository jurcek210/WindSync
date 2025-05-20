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

    fun getToken(): Token {
        skipWhitespace()

        val startColumn = column
        val startRow = row
        val c = peekChar()
        if (c == -1) return Token(Symbol.EOF, "", startRow, startColumn)

        val ch = c.toChar()

        // === IDENTIFIKATOR (VARIABLE) ali KLJUČNA BESEDA ===
        if (ch.isLetter() || ch == '_') {
            val buffer = StringBuilder()
            while (peekChar().toChar().isLetterOrDigit() || peekChar().toChar() == '_') {
                buffer.append(readChar().toChar())
            }
            val lexeme = buffer.toString()
            val keyword = when (lexeme) {
                "true" -> Symbol.TRUE
                "false" -> Symbol.FALSE
                "set" -> Symbol.SET
                "bend" -> Symbol.BEND
                "circle" -> Symbol.CIRCLE
                "connect" -> Symbol.CONNECT
                "box" -> Symbol.BOX
                "point" -> Symbol.POINT
                "get" -> Symbol.GET
                "marker" -> Symbol.MARKER
                "line" -> Symbol.LINE
                "area" -> Symbol.AREA
                "count" -> Symbol.COUNT
                "zelenamreza" -> Symbol.ZELENAMREZA
                "link" -> Symbol.LINK
                else -> Symbol.VARIABLE
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
            return Token(Symbol.STRING, buffer.toString(), startRow, startColumn)
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
            return Token(Symbol.REAL, buffer.toString(), startRow, startColumn)
        }

        // === ENOZNAČNI SIMBOLI ===
        when (readChar().toChar()) {
            '+' -> return Token(Symbol.PLUS, "+", startRow, startColumn)
            '-' -> {
                // Preverimo ali je to sladkorček --
                if (peekChar().toChar() == '-') {
                    readChar()
                    return Token(Symbol.LINK, "--", startRow, startColumn)
                }
                return Token(Symbol.MINUS, "-", startRow, startColumn)
            }
            '*' -> return Token(Symbol.TIMES, "*", startRow, startColumn)
            '/' -> return Token(Symbol.DIVIDES, "/", startRow, startColumn)
            ':' -> return Token(Symbol.COLON, ":", startRow, startColumn)
            '=' -> return Token(Symbol.ASSIGN, "=", startRow, startColumn)
            '(' -> return Token(Symbol.LPAREN, "(", startRow, startColumn)
            ')' -> return Token(Symbol.RPAREN, ")", startRow, startColumn)
            '{' -> return Token(Symbol.BEGIN, "{", startRow, startColumn)
            '}' -> return Token(Symbol.END, "}", startRow, startColumn)
            '.' -> return Token(Symbol.POINT, ".", startRow, startColumn)
            ';' -> return Token(Symbol.SEMICOLON, ";", startRow, startColumn)
            ',' -> return Token(Symbol.TO, ";", startRow, startColumn)

        }

        return Token(Symbol.VARIABLE, readChar().toChar().toString(), startRow, startColumn)
    }


    private fun skipWhitespace() {
        while (true) {
            val c = peekChar()
            if (c == -1) break
            if (c.toChar().isWhitespace()) readChar() else break
        }
    }

}
