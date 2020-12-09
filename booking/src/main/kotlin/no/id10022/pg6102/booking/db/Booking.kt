package no.id10022.pg6102.booking.db

import no.id10022.pg6102.booking.dto.BookingDto
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@Entity
@Table(
    name = "BOOKINGS"
)
class Booking(

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get:NotNull
    @get:ManyToOne
    var user: User,

    @get:NotNull
    @get:ManyToOne
    var trip: Trip,

    @get:NotNull
    @get:Min(1, message = "Amount cannot be negative")
    @get:Max(Int.MAX_VALUE.toLong(), message = "Amount cannot be greater than ${Int.MAX_VALUE}")
    var amount: Int = 0,

    @get:NotNull
    var cancelled: Boolean = false

)

fun Booking.toDto(): BookingDto {
    return BookingDto(id, user.username, trip.id, amount, cancelled)
}