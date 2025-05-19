import java.io.File
import java.io.FileInputStream

fun main() {
    val input=File("test.txt")
    val inputStream =FileInputStream(input)
    val scanner = Scanner(inputStream)
    val parser = Parser(scanner)
    parser.parse()

/*
    println("Tokens:")
    while (true) {
        val token = scanner.getToken()
        println(token)
        if (token.symbol == Symbol.EOF) break
    }

 */
}