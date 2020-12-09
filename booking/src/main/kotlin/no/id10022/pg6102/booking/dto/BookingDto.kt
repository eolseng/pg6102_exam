package no.id10022.pg6102.booking.dto

import io.swagger.annotations.ApiModelProperty

data class BookingDto(

    @get:ApiModelProperty("The ID of the Booking")
    var id: Long? = null,

    @get:ApiModelProperty("The username of the User")
    var username: String? = null,

    @get:ApiModelProperty("The ID of the Trip")
    var tripId: Long? = null,

    @get:ApiModelProperty("The amount of reservations for the Booking")
    var amount: Int? = null,

    @get:ApiModelProperty("The status of the Booking")
    var cancelled: Boolean? = null

)