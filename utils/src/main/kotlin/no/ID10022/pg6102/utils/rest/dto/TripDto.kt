package no.id10022.pg6102.utils.rest.dto

import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

data class TripDto(

    @get:ApiModelProperty("The ID of the Trip")
    var id: Long? = null,

    @get:ApiModelProperty("The title of the Trip")
    var title: String? = null,

    @get:ApiModelProperty("The description of the Trip")
    var description: String? = null,

    @get:ApiModelProperty("The location of the Trip")
    var location: String? = null,

    @get:ApiModelProperty("The start of the Trip")
    var start: LocalDateTime? = null,

    @get:ApiModelProperty("The end of the Trip")
    var end: LocalDateTime? = null,

    @get:ApiModelProperty("The duration of the Trip")
    var duration: Map<String, Int>? = null,

    @get:ApiModelProperty("The price per person of the Trip")
    var price: Int? = null,

    @get:ApiModelProperty("The capacity of the Trip")
    var capacity: Int? = null

)

fun TripDto.isValidForRegistration(): Boolean {

    // Check for null-values
    val title = this.title ?: return false
    val description = this.description ?: return false
    val location = this.location ?: return false
    val start = this.start ?: return false
    val end = this.end ?: return false
    val price = this.price ?: return false
    val capacity = this.capacity ?: return false

    // Other constraints
    if (title.isEmpty()) return false
    if (description.isEmpty()) return false
    if (location.isEmpty()) return false
    if (start < LocalDateTime.now()) return false
    if (end < start) return false
    if (price < 0 || price > Int.MAX_VALUE) return false
    if (capacity < 1 || capacity > Int.MAX_VALUE) return false

    // DTO is good for registration
    return true
}
