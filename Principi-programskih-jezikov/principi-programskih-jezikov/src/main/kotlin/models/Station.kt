import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import models.Location
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Station(
    @Contextual val id: UUID = UUID.randomUUID(),
    var name: String = "",
    val location: Location,
    var windSpeed: Double? = null,
    var status: Boolean = true,
    val createdAt: String = LocalDateTime.now().toString()
)
