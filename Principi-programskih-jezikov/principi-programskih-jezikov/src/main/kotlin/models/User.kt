package models

import io.github.serpro69.kfaker.Faker
import java.time.LocalDateTime
import java.util.*

class User(
    val id: UUID = UUID.randomUUID(),
    val username: String,
    val email: String,
    private var password: String,
    val stations: Stations = Stations(),
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    companion object {
        private val faker = Faker()

        fun randomUser(): User {
            val username = faker.name.name()
            val email = faker.internet.safeEmail()
            val password = faker.random.nextString(10)
            return User(username = username, email = email, password = password)
        }
    }

    override fun toString(): String {
        return "$username <$email> (${stations.size()} veternic) - ustvarjen: $createdAt"
    }
}
