package no.id10022.pg6102.booking.db

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "USERS")
class User (

    @get:Id
    @get:NotBlank
    var username: String? = null,

    @get:OneToMany(mappedBy = "user", cascade = [(CascadeType.ALL)])
    var bookings: MutableList<Booking> = mutableListOf()

)