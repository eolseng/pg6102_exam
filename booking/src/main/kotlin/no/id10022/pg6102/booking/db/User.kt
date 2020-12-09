package no.id10022.pg6102.booking.db

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "USERS")
class User (

    @get:Id
    @get:NotBlank
    var username: String

)