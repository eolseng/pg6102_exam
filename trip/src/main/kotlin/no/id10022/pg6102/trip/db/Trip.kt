package no.id10022.pg6102.trip.db

import no.id10022.pg6102.trip.dto.TripDto
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(name = "TRIPS")
class Trip(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @get:NotBlank(message = "Trip must have title")
    var title: String = "",

    @get:NotBlank(message = "Trip must have description")
    var description: String = "",

    @get:NotBlank(message = "Trip must have a location")
    var location: String = "",

    @get:NotNull
    var start: LocalDateTime,

    @get:NotNull
    var end: LocalDateTime,

    @get:NotNull
    @get:Min(0, message = "Price cannot be negative")
    @get:Max(Int.MAX_VALUE.toLong(), message = "Price cannot be greater than ${Int.MAX_VALUE}")
    var price: Int = 0,

    @get:NotNull
    @get:Min(1, message = "Capacity cannot be negative")
    @get:Max(Int.MAX_VALUE.toLong(), message = "Capacity cannot be greater than ${Int.MAX_VALUE}")
    var capacity: Int = 0

) {
    @Transient
    var duration: Map<String, Int> = mapOf()
        private set
        get() {
            val map = mutableMapOf<String, Int>()
            val days = start.until(end, ChronoUnit.DAYS)
            val hours = start.until(end, ChronoUnit.HOURS) - (days * 24)
            val minutes = start.until(end, ChronoUnit.MINUTES) - (days * 24 * 60) - (hours * 60)
            map["days"] = days.toInt()
            map["hours"] = hours.toInt()
            map["minutes"] = minutes.toInt()
            return map
        }
}

fun Trip.toDto(): TripDto {
    return TripDto(id, title, description, location, start, end, duration, price, capacity)
}