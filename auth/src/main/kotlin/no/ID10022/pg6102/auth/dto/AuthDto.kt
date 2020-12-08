package no.id10022.pg6102.auth.dto

import javax.validation.constraints.NotBlank

data class AuthDto(

    @get:NotBlank
    var username: String,

    @get:NotBlank
    val password: String

)