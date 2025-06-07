import com.mongodb.*
import com.mongodb.MongoClientSettings
import io.github.cdimascio.dotenv.dotenv
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import models.User
import org.bson.UuidRepresentation

import models.Location
import models.Station
import org.litote.kmongo.getCollection

object Database {
    private val dotenv = dotenv()
    private val uri = dotenv["MONGO_URI"] ?: error("MONGO_URI is missing in .env")

    val connectionString = ConnectionString(uri)
    val settings = MongoClientSettings.builder()
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .applyConnectionString(connectionString)
        .build()
    val client = KMongo.createClient(settings)
    val database = client.getDatabase("test")

    val users = database.getCollection<User>("users")

    val windmills = database.getCollection<Station>("windmills")
    //val windmills = database.getCollection<Station>("test") //za test da če dela


}
