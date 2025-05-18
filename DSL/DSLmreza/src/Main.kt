import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

fun main() {
    val input=File("test.txt")
    val inputStream =FileInputStream(input)
    val scanner = Scanner(inputStream)

    println("Tokens:")
    while (true) {
        val token = scanner.nextToken()
        println(token)
        if (token.symbol == SymbolType.EOF) break
    }
}