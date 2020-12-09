package no.id10022.pg6102.booking

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.id10022.pg6102.booking.db.BookingRepository
import no.id10022.pg6102.booking.db.toDto
import no.id10022.pg6102.booking.dto.BookingDto
import no.id10022.pg6102.booking.service.BookingService
import no.id10022.pg6102.booking.service.TripService
import no.id10022.pg6102.booking.service.UserService
import no.id10022.pg6102.utils.rest.RestResponseFactory
import no.id10022.pg6102.utils.rest.WrappedResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.net.URI

const val BOOKINGS_API_PATH = "$API_BASE_PATH/bookings"

@Api(value = BOOKINGS_API_PATH, description = "Endpoint for managing Bookings")
@RestController
@RequestMapping(
    path = [BOOKINGS_API_PATH],
    produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
class RestApi(
    private val repository: BookingRepository,
    private val bookingService: BookingService,
    private val userService: UserService,
    private val tripService: TripService
) {

    @ApiOperation("Create a Booking")
    @PostMapping
    fun createBooking(
        @ApiParam("Data for the new Booking")
        @RequestBody dto: BookingDto,
        auth: Authentication
    ): ResponseEntity<WrappedResponse<Void>> {
        // Validate DTO
        when {
            dto.id != null ->
                return RestResponseFactory.userError("Invalid data - must not contain field 'id'")
            dto.cancelled != null ->
                return RestResponseFactory.userError("Invalid data - must not contain field 'cancelled'")
            dto.tripId == null ->
                return RestResponseFactory.userError("Invalid data - must contain field 'tripId'")
            dto.username == null ->
                return RestResponseFactory.userError("Invalid data - must contain field 'username'")
            dto.username != auth.name ->
                return RestResponseFactory.userError("Invalid data - cannot register Booking on other users", 403)
            dto.amount == null ->
                return RestResponseFactory.userError("Invalid data - must contain field 'amount'")
            dto.amount!! < 1 ->
                return RestResponseFactory.userError("Invalid data - 'amount' cannot be 0 or negative")
            dto.amount!! > Int.MAX_VALUE ->
                return RestResponseFactory.userError("Invalid data - 'amount' cannot be greater than ${Int.MAX_VALUE}")
        }
        // Create the Booking
        val booking = try {
            bookingService.createBooking(dto.username!!, dto.tripId!!, dto.amount!!)
        } catch (e: IllegalArgumentException) {
            // Failed to create Booking
            return RestResponseFactory.userError(e.message.orEmpty())
        }
        // Return path to the created Booking
        return RestResponseFactory.created(URI.create("$BOOKINGS_API_PATH/${booking.id}"))
    }

    @ApiOperation("Retrieve a specific Booking by the ID")
    @GetMapping("/{id}")
    fun getBookingById(
        @ApiParam("The ID of the Booking to retrieve")
        @PathVariable("id") id: Long,
        auth: Authentication
    ): ResponseEntity<WrappedResponse<Any>> {
        // Retrieve Booking from the database
        val booking = repository.findByIdOrNull(id)
            ?: return RestResponseFactory.notFound("Could not find Booking with ID $id")
        // Check that authenticated user is either owner or admin
        if (auth.name != booking.user.username && !auth.hasRole("ADMIN"))
            return RestResponseFactory.userError("Cannot retrieve other users Bookings", 403)
        // Convert to DTO
        val dto = booking.toDto()
        // Send the DTO
        return RestResponseFactory.payload(200, dto)
    }

    // Utility function to check if Authenticated User has a given role. Use "ADMIN", not "ROLE_ADMIN".
    fun Authentication.hasRole(role: String): Boolean {
        return this.authorities.stream().anyMatch { it.authority == role }
    }

}