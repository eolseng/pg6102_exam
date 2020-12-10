package no.id10022.pg6102.booking.dto

import io.swagger.annotations.ApiModelProperty

enum class Command{
    CANCEL,
    UPDATE_AMOUNT
}

data class PatchBookingDto(

    @get:ApiModelProperty("Command to execute on the Booking")
    var command: Command? = null,

    @get:ApiModelProperty("Optional amount to update the Booking to")
    var newAmount: Int? = null

)
