    class Parser(
        private val lex: Scanner,
        private var currentToken: Token = lex.getToken()
    ) {

    }
