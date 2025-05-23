import AST.Point
import AST.*
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import com.google.gson.GsonBuilder
/*toti test dela:
* zelenamreza "primer" {
 let p = point(10, 20);
  let center = point(75, 75);
   senzor "s1" {
        marker(p) (color = red, label = "start", value = 100);
    };

     kabel "k1" {
            bend((100,100), (200,200), 30);
        };
     proizvajalci "p1" {
            box((50,50), (150,150));
        };
    baterija "b1" {
        circle(center, 25);
    };
    connect("k1", "p1");

};
* */


fun main() {
    val input = File("test.txt")
    val inputStream = FileInputStream(input)
    val scanner = Scanner(inputStream)
    val parser = Parser(scanner)

    parser.parse()
    val featureCollection = mapOf(
        "type" to "FeatureCollection",
        "features" to commands.map { it.eval() }
    )
    println(Gson().toJson(featureCollection))

    val features = commands.map { it.eval() }

    val geoJson = mapOf(
        "type" to "FeatureCollection",
        "features" to features
    )

    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonOutput = gson.toJson(geoJson)

    println(jsonOutput)
}
