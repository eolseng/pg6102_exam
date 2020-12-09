package no.id10022.pg6102.booking.db

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "TRIPS")
class Trip(

    @get:Id
    @get:NotNull
    var id: Long

)