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

    private object State {
        const val START = 0
        const val IDENTIFIER = 1
        const val NUMBER = 2
        const val NUMBER_FRACTION = 3
        const val STRING = 4
        const val MINUS = 5
        const val MINUS2 = 6
        const val DONE = 7
        const val SYMBOL = 8
        const val EOF = 9
    }

    private val keywords = mapOf(
        "true" to Symbol.TRUE,
        "false" to Symbol.FALSE,
        "set" to Symbol.SET,
        "bend" to Symbol.BEND,
        "circle" to Symbol.CIRCLE,
        "connect" to Symbol.CONNECT,
        "box" to Symbol.BOX,
        "point" to Symbol.POINT,
        "get" to Symbol.GET,
        "marker" to Symbol.MARKER,
        "line" to Symbol.LINE,
        "area" to Symbol.AREA,
        "count" to Symbol.COUNT,
        "zelenamreza" to Symbol.ZELENAMREZA,
        "kabel" to Symbol.KABEL,
        "proizvajalci" to Symbol.PROIZVAJALCI,
        "senzor" to Symbol.SENZOR,
        "baterija" to Symbol.BATERIJA,
        "let" to Symbol.LET,
        "color" to Symbol.COLOR,
        "red" to Symbol.RED,
        "green" to Symbol.GREEN,
        "blue" to Symbol.BLUE,
        "yellow" to Symbol.YELLOW,
        "black" to Symbol.BLACK,
        "white" to Symbol.WHITE,
        "gray" to Symbol.GRAY,
        "purple" to Symbol.PURPLE,
        "orange" to Symbol.ORANGE,
        "pink" to Symbol.PINK,
        "brown" to Symbol.BROWN,
        "cyan" to Symbol.CYAN,
        "magenta" to Symbol.MAGENTA,
        "rgb" to Symbol.RGB,
        "value" to Symbol.VALUE,
        "label" to Symbol.LABEL
    )

    fun getToken(): Token {
        skipWhitespace()
        val startRow = row
        val startColumn = column

        var state = State.START
        val buffer = StringBuilder()

        while (true) {
            val c = peekChar()
            val ch = if (c != -1) c.toChar() else null

            when (state) {
                State.START -> {
                    if (c == -1) {
                        return Token(Symbol.EOF, "", startRow, startColumn)
                    }
                    when {
                        ch!!.isLetter() || ch == '_' -> {
                            buffer.append(readChar().toChar())
                            state = State.IDENTIFIER
                        }
                        ch.isDigit() -> {
                            buffer.append(readChar().toChar())
                            state = State.NUMBER
                        }
                        ch == '"' -> {
                            readChar()
                            state = State.STRING
                        }
                        ch == '-' -> {
                            readChar()
                            val next = peekChar()
                            if (next.toChar() == '-') {
                                readChar()
                                return Token(Symbol.LINK, "--", startRow, startColumn)
                            } else {
                                return Token(Symbol.MINUS, "-", startRow, startColumn)
                            }
                        }
                        else -> {
                            readChar()
                            return when (ch) {
                                '+' -> Token(Symbol.PLUS, "+", startRow, startColumn)
                                '*' -> Token(Symbol.TIMES, "*", startRow, startColumn)
                                '/' -> Token(Symbol.DIVIDES, "/", startRow, startColumn)
                                ':' -> Token(Symbol.COLON, ":", startRow, startColumn)
                                '=' -> Token(Symbol.ASSIGN, "=", startRow, startColumn)
                                '(' -> Token(Symbol.LPAREN, "(", startRow, startColumn)
                                ')' -> Token(Symbol.RPAREN, ")", startRow, startColumn)
                                '{' -> Token(Symbol.BEGIN, "{", startRow, startColumn)
                                '}' -> Token(Symbol.END, "}", startRow, startColumn)
                                '.' -> Token(Symbol.POINT, ".", startRow, startColumn)
                                ';' -> Token(Symbol.SEMICOLON, ";", startRow, startColumn)
                                ',' -> Token(Symbol.TO, ",", startRow, startColumn)
                                else -> Token(Symbol.VARIABLE, ch.toString(), startRow, startColumn)
                            }
                        }
                    }
                }

                State.IDENTIFIER -> {
                    while (true) {
                        val next = peekChar()
                        if (next == -1) break
                        val nChar = next.toChar()
                        if (nChar.isLetterOrDigit() || nChar == '_') {
                            buffer.append(readChar().toChar())
                        } else {
                            break
                        }
                    }
                    val lexeme = buffer.toString()
                    val keyword = keywords[lexeme] ?: Symbol.VARIABLE
                    return Token(keyword, lexeme, startRow, startColumn)
                }

                State.NUMBER -> {
                    var hasDot = false
                    while (true) {
                        val next = peekChar()
                        if (next == -1) break
                        val nChar = next.toChar()
                        if (nChar.isDigit()) {
                            buffer.append(readChar().toChar())
                        } else if (nChar == '.' && !hasDot) {
                            hasDot = true
                            buffer.append(readChar().toChar())
                        } else {
                            break
                        }
                    }
                    return Token(Symbol.REAL, buffer.toString(), startRow, startColumn)
                }

                State.STRING -> {
                    while (true) {
                        val next = peekChar()
                        if (next == -1) {
                            break
                        }
                        if (next.toChar() == '"') {
                            readChar() // consume closing "
                            break
                        }
                        buffer.append(readChar().toChar())
                    }
                    return Token(Symbol.STRING, buffer.toString(), startRow, startColumn)
                }

                else -> {

                    throw IllegalStateException("Invalid lexer state")
                }
            }
        }
    }

    private fun skipWhitespace() {
        while (true) {
            val c = peekChar()
            if (c == -1) break
            if (c.toChar().isWhitespace()) {
                readChar()
            } else {
                break
            }
        }
    }
}
