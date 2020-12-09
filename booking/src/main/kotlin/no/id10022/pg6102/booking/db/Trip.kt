package no.id10022.pg6102.booking.db

import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "TRIPS")
class Trip(

    @get:Id
    @get:NotNull
    var id: Long? = null,

    @get:OneToMany(mappedBy = "trip", cascade = [(CascadeType.ALL)])
    var bookings: MutableList<Booking> = mutableListOf(),

    @get:NotNull
    var cancelled: Boolean = false

)