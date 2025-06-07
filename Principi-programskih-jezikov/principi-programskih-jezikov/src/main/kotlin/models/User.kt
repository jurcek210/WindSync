package models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class User(
    @Contextual val _id: ObjectId = ObjectId(),
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val stations: List<Station> = emptyList()
)
