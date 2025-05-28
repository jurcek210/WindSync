package org.example
import Token
import java.io.InputStream
import Symbol

class Lexer
    (inputStream: InputStream) {
    private val reader = inputStream.bufferedReader()
    private var peekedChar: Int? = null
    private var row = 1
    private var column = 0

    private val states = (0..330).toSet()
    private val alphabet = (0..255)
    private val startState = 0
    private val numberOfStates = states.max()!! + 1
    private val numberOfCodes = alphabet.max()!! + 1
    private val transitions = Array(numberOfStates) { IntArray(numberOfCodes) }
    private val values = Array(numberOfStates) { Symbol.IGNORE }

    init {


        // Numbers (REAL)
        for (c in '0'..'9') setTransition(0, c, 2)
        for (c in '0'..'9') setTransition(2, c, 2)
        setTransition(2, '.', 3)
        for (c in '0'..'9') setTransition(3, c, 4)
        for (c in '0'..'9') setTransition(4, c, 4)
        setSymbol(2, Symbol.REAL)
        setSymbol(4, Symbol.REAL)

        // Strings
        setTransition(0, '"', 5)
        for (i in 32..126) if (i != '"'.code) setTransition(5, i.toChar(), 5)
        setTransition(5, '"', 6)
        setSymbol(6, Symbol.STRING)

        // Operators & punctuation
        setTransition(0, '+', 10); setSymbol(10, Symbol.PLUS)
        setTransition(0, '-', 11)
        setTransition(11, '-', 12); setSymbol(12, Symbol.LINK)
        setSymbol(11, Symbol.MINUS)
        setTransition(0, '*', 13); setSymbol(13, Symbol.TIMES)
        setTransition(0, '/', 14); setSymbol(14, Symbol.DIVIDES)
        setTransition(0, ':', 15); setSymbol(15, Symbol.COLON)
        setTransition(0, '=', 16); setSymbol(16, Symbol.ASSIGN)
        setTransition(0, '(', 17); setSymbol(17, Symbol.LPAREN)
        setTransition(0, ')', 18); setSymbol(18, Symbol.RPAREN)
        setTransition(0, '{', 19); setSymbol(19, Symbol.BEGIN)
        setTransition(0, '}', 20); setSymbol(20, Symbol.END)
        setTransition(0, '.', 21); setSymbol(21, Symbol.POINT)
        setTransition(0, ';', 22); setSymbol(22, Symbol.SEMICOLON)
        setTransition(0, ',', 23); setSymbol(23, Symbol.TO)

        setTransition(0, 'l', 24)
        setTransition(24, 'e', 25)
        setTransition(25, 't', 26)
        setSymbol(26, Symbol.LET)


        // false
        setTransition(0, 'f', 27)
        setTransition(27, 'a', 28)
        setTransition(28, 'l', 29)
        setTransition(29, 's', 30)
        setTransition(30, 'e', 31)
        setSymbol(31, Symbol.FALSE)

// set
        setTransition(0, 's', 32)
        setTransition(32, 'e', 33)
        setTransition(33, 't', 34)
        setSymbol(34, Symbol.SET)

// get
        setTransition(0, 'g', 35)
        setTransition(35, 'e', 36)
        setTransition(36, 't', 37)
        setSymbol(37, Symbol.GET)

// box
        setTransition(0, 'b', 38)
        setTransition(38, 'o', 39)
        setTransition(39, 'x', 40)
        setSymbol(40, Symbol.BOX)

// point
        setTransition(0, 'p', 41)
        setTransition(41, 'o', 42)
        setTransition(42, 'i', 43)
        setTransition(43, 'n', 44)
        setTransition(44, 't', 45)
        setSymbol(45, Symbol.POINT)

// marker
        setTransition(0, 'm', 46)
        setTransition(46, 'a', 47)
        setTransition(47, 'r', 48)
        setTransition(48, 'k', 49)
        setTransition(49, 'e', 50)
        setTransition(50, 'r', 51)
        setSymbol(51, Symbol.MARKER)

// line
        setTransition(24, 'i', 53)
        setTransition(53, 'n', 54)
        setTransition(54, 'e', 55)
        setSymbol(55, Symbol.LINE)

// area
        setTransition(0, 'a', 56)
        setTransition(56, 'r', 57)
        setTransition(57, 'e', 58)
        setTransition(58, 'a', 59)
        setSymbol(59, Symbol.AREA)

// count
        setTransition(0, 'c', 60)
        setTransition(60, 'o', 61)
        setTransition(61, 'u', 62)
        setTransition(62, 'n', 63)
        setTransition(63, 't', 64)
        setSymbol(64, Symbol.COUNT)

        // connect
        setTransition(61, 'n', 205)
        setTransition(205, 'n', 206)
        setTransition(206, 'e', 207)
        setTransition(207, 'c', 208)
        setTransition(208, 't', 209)
        setSymbol(209, Symbol.CONNECT)

// true
        setTransition(0, 't', 65)
        setTransition(65, 'r', 66)
        setTransition(66, 'u', 67)
        setTransition(67, 'e', 68)
        setSymbol(68, Symbol.TRUE)

        // zelenamreza
        setTransition(0, 'z', 69)
        setTransition(69, 'e', 70)
        setTransition(70, 'l', 71)
        setTransition(71, 'e', 72)
        setTransition(72, 'n', 73)
        setTransition(73, 'a', 74)
        setTransition(74, 'm', 75)
        setTransition(75, 'r', 76)
        setTransition(76, 'e', 77)
        setTransition(77, 'z', 78)
        setTransition(78, 'a', 79)
        setSymbol(79, Symbol.ZELENAMREZA)

// kabel
        setTransition(0, 'k', 80)
        setTransition(80, 'a', 81)
        setTransition(81, 'b', 82)
        setTransition(82, 'e', 83)
        setTransition(83, 'l', 84)
        setSymbol(84, Symbol.KABEL)

// proizvajalci
        setTransition(41, 'r', 86)
        setTransition(86, 'o', 87)
        setTransition(87, 'i', 88)
        setTransition(88, 'z', 89)
        setTransition(89, 'v', 90)
        setTransition(90, 'a', 91)
        setTransition(91, 'j', 92)
        setTransition(92, 'a', 93)
        setTransition(93, 'l', 94)
        setTransition(94, 'c', 95)
        setTransition(95, 'i', 96)
        setSymbol(96, Symbol.PROIZVAJALCI)

// senzor
        setTransition(33, 'n', 99)
        setTransition(99, 'z', 100)
        setTransition(100, 'o', 101)
        setTransition(101, 'r', 102)
        setSymbol(102, Symbol.SENZOR)

// baterija
        setTransition(38, 'a', 104)
        setTransition(104, 't', 105)
        setTransition(105, 'e', 106)
        setTransition(106, 'r', 107)
        setTransition(107, 'i', 108)
        setTransition(108, 'j', 109)
        setTransition(109, 'a', 110)
        setSymbol(110, Symbol.BATERIJA)






// value
        setTransition(0, 'v', 183)
        setTransition(183, 'a', 184)
        setTransition(184, 'l', 185)
        setTransition(185, 'u', 186)
        setTransition(186, 'e', 187)
        setSymbol(187, Symbol.VALUE)

// label

        setTransition(24, 'a', 189)
        setTransition(189, 'b', 190)
        setTransition(190, 'e', 191)
        setTransition(191, 'l', 192)
        setSymbol(192, Symbol.LABEL)

// bend
        setTransition(38, 'e', 194)
        setTransition(194, 'n', 195)
        setTransition(195, 'd', 196)
        setSymbol(196, Symbol.BEND)

// circle
        setTransition(60, 'i', 198)
        setTransition(198, 'r', 199)
        setTransition(199, 'c', 200)
        setTransition(200, 'l', 201)
        setTransition(201, 'e', 202)
        setSymbol(202, Symbol.CIRCLE)
//barve
        //orange

        setTransition(0, 'o', 203)
        setTransition(203, 'r', 204)
        setTransition(204, 'a', 205)
        setTransition(205, 'n', 206)
        setTransition(206, 'g', 207)
        setTransition(207, 'e', 208)
        setSymbol(208, Symbol.ORANGE)

        //collor

        setTransition(61, 'l', 211)
        setTransition(211, 'o', 212)
        setTransition(212, 'r', 213)
        setSymbol(213, Symbol.COLOR)

        //red

        setTransition(0, 'r', 214)
        setTransition(214, 'e', 215)
        setTransition(215, 'd', 216)
        setSymbol(216, Symbol.RED)

        //green

        setTransition(35, 'r', 217)
        setTransition(217, 'e', 218)
        setTransition(218, 'e', 219)
        setTransition(219, 'n', 220)
        setSymbol(220, Symbol.GREEN)

        //blue

        setTransition(38, 'l', 221)
        setTransition(221, 'u', 222)
        setTransition(222, 'e', 223)
        setSymbol(223, Symbol.CYAN)

        //yellow

        setTransition(0, 'y', 224)
        setTransition(224, 'e', 225)
        setTransition(225, 'l', 226)
        setTransition(226, 'l', 227)
        setTransition(227, 'o', 228)
        setTransition(228, 'w', 229)
        setSymbol(229, Symbol.YELLOW)

        // cyan
        setTransition(60, 'y', 230)
        setTransition(230, 'a', 231)
        setTransition(231, 'n', 232)
        setSymbol(232, Symbol.BLUE)

        //brown
        setTransition(38, 'r', 233)
        setTransition(233, 'o', 234)
        setTransition(234, 'w', 235)
        setTransition(235, 'n', 236)
        setSymbol(236, Symbol.BROWN)

        //black

        setTransition(221, 'a', 238)
        setTransition(238, 'c', 239)
        setTransition(239, 'k', 240)
        setSymbol(240, Symbol.BLACK)




// rgb
        setTransition(214, 'g', 217)
        setTransition(217, 'b', 218)
        setSymbol(218, Symbol.RGB)

        for (c in 'a'..'z') setTransition(300, c, 300)
        for (c in 'A'..'Z') setTransition(300, c, 300)
        for (c in '0'..'9') setTransition(300, c, 300)
        for (c in 'a'..'z') if (transitions[0][c.code] == 0) setTransition(0, c, 300)
        for (c in 'A'..'Z') if (transitions[0][c.code] == 0) setTransition(0, c, 300)
        if (transitions[0]['_'.code] == 0) setTransition(0, '_', 300)

        setTransition(300, '_', 300)
        setSymbol(300, Symbol.VARIABLE)
        val reservedPaths = listOf(24, 27, 32, 35, 38, 41, 46, 56, 60, 65, 69, 80, 183, 203, 214, 224)
        for (state in reservedPaths) {
            for (c in 'a'..'z') {
                if (transitions[state][c.code] == 0) {
                    setTransition(state, c, 300)
                }
            }
            for (c in 'A'..'Z') {
                if (transitions[state][c.code] == 0) {
                    setTransition(state, c, 300)
                }
            }
            for (c in '0'..'9') {
                if (transitions[state][c.code] == 0) {
                    setTransition(state, c, 300)
                }
            }
            if (transitions[state]['_'.code] == 0) {
                setTransition(state, '_', 300)
            }
        }


    }

    private fun setTransition(from: Int, ch: Char, to: Int) {
        transitions[from][ch.code] = to
    }

    private fun setSymbol(state: Int, symbol: Symbol) {
        values[state] = symbol
    }

    private fun next(state: Int, code: Int): Int {
        return if (state in states && code in 0..255) transitions[state][code] else 0
    }

    private fun symbol(state: Int): Symbol {
        return values[state]
    }

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

    private fun skipWhitespace() {
        while (true) {
            val c = peekChar()
            if (c == -1 || !c.toChar().isWhitespace()) break
            readChar()
        }
    }

    fun getToken(): Token {
        skipWhitespace()
        val startRow = row
        val startColumn = column
        val buffer = StringBuilder()

        var state = startState
        var lastFinalState = -1
        var lastFinalIndex = -1

        var index = 0
        while (true) {
            val c = peekChar()
            if (c == -1) break
            val nextState = next(state, c)
            if (nextState == 0) break

            buffer.append(readChar().toChar())
            state = nextState

            if (symbol(state) != Symbol.IGNORE) {
                lastFinalState = state
                lastFinalIndex = index + 1
            }
            index++
        }
        if (lastFinalState < 0) {
            val c = peekChar()
            if (c == -1) {
                return Token(Symbol.EOF, "", startRow, startColumn)
            } else {
                throw IllegalStateException("No valid token found at row $startRow, column $startColumn. Got: '${buffer.toString()}'")
            }
        }


        val rawText = buffer.toString().take(lastFinalIndex)
        val finalSymbol = symbol(lastFinalState)

        val finalText = if (finalSymbol == Symbol.STRING)
            rawText.removeSurrounding("\"")
        else
            rawText



        return Token(finalSymbol, finalText, startRow, startColumn)

    }

}