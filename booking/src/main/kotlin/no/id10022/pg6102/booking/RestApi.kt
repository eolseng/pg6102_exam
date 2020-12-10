package no.id10022.pg6102.booking

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.id10022.pg6102.booking.db.BookingRepository
import no.id10022.pg6102.booking.db.toDto
import no.id10022.pg6102.booking.dto.BookingDto
import no.id10022.pg6102.booking.dto.Command
import no.id10022.pg6102.booking.dto.PatchBookingDto
import no.id10022.pg6102.booking.service.BookingService
import no.id10022.pg6102.booking.service.MAX_BOOKING_AMOUNT
import no.id10022.pg6102.booking.service.MIN_BOOKING_AMOUNT
import no.id10022.pg6102.utils.rest.RestResponseFactory
import no.id10022.pg6102.utils.rest.WrappedResponse
import no.id10022.pg6102.utils.rest.dto.PageDto
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
    private val bookingService: BookingService
) {

    @PostMapping
    @ApiOperation("Create a Booking")
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
            dto.amount!! < MIN_BOOKING_AMOUNT ->
                return RestResponseFactory.userError("Invalid data - 'amount' cannot be less than $MIN_BOOKING_AMOUNT")
            dto.amount!! > MAX_BOOKING_AMOUNT ->
                return RestResponseFactory.userError("Invalid data - 'amount' cannot be greater than $MAX_BOOKING_AMOUNT")
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

    @GetMapping("/{id}")
    @ApiOperation("Retrieve a specific Booking by the ID")
    fun getBookingById(
        @ApiParam("The ID of the Booking to retrieve")
        @PathVariable("id") id: Long,
        auth: Authentication
    ): ResponseEntity<WrappedResponse<Any>> {
        // Retrieve Booking from the database
        val booking = repository.findByIdOrNull(id)
            ?: return RestResponseFactory.notFound("Could not find Booking with ID $id")
        // Check that authenticated user is either owner or admin
        if (!auth.hasRole("ADMIN") && auth.name != booking.user.username)
            return RestResponseFactory.userError("Cannot retrieve other users Bookings", 403)
        // Convert to DTO
        val dto = booking.toDto()
        // Send the DTO
        return RestResponseFactory.payload(200, dto)
    }

    @GetMapping
    @ApiOperation("Retrieve all bookings, sorted by ID")
    fun getAllBookings(
        @RequestParam("keysetId", required = false)
        keysetId: Long?,
        @RequestParam("amount", required = false)
        amountParam: Int?,
        auth: Authentication
    ): ResponseEntity<WrappedResponse<PageDto<BookingDto>>> {
        // Set amount if not supplied
        val amount = amountParam ?: 10
        // Verify amount is in range
        if (amount !in 1..1000)
            return RestResponseFactory.userError("Amount must be between in the range of 1 to 1000")
        // Fetch Bookings and convert to DTOs
        val dtos = bookingService.getNextPage(auth.name, keysetId, amount).map { it.toDto() }
        // Create PageDto with Bookings
        val page = PageDto(list = dtos)
        // Set next if more
        if (dtos.size == amount)
            page.next = "$BOOKINGS_API_PATH?keysetId=${dtos.last().id}&amount=$amount"
        // Return the Page
        return RestResponseFactory.payload(200, page)
    }

    @PatchMapping(
        path = ["/{id}"],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    @ApiOperation("Update a Booking")
    fun patchBooking(
        @ApiParam("The ID of the Booking to patch")
        @PathVariable("id") id: Long,
        @RequestBody dto: PatchBookingDto,
        auth: Authentication
    ): ResponseEntity<WrappedResponse<Void>> {
        // Get Booking
        val booking = repository.findByIdOrNull(id)
            ?: return RestResponseFactory.notFound("Could not find Booking with ID $id")
        // Check that authenticated user is either owner or admin
        if (!auth.hasRole("ADMIN") && auth.name != booking.user.username)
            return RestResponseFactory.userError("Cannot patch other users Bookings", 403)
        // Validate and execute command
        when (dto.command) {
            null ->
                return RestResponseFactory.userError("Invalid data - must contain field 'command'")
            Command.CANCEL ->
                bookingService.cancelBooking(id)
            Command.UPDATE_AMOUNT -> {
                val newAmount = dto.newAmount
                    ?: return RestResponseFactory.userError("Invalid data - must contain field 'newAmount'")
                try {
                    bookingService.updateAmount(id, newAmount)
                } catch (e: java.lang.IllegalArgumentException) {
                    // Failed to update amount on Booking
                    return RestResponseFactory.userError(e.message.orEmpty())
                }
            }
        }
        return RestResponseFactory.noPayload(204)
    }

    /**
     *  Utility function to check if Authentication has a given role.
     *  Use e.g. "ADMIN", not "ROLE_ADMIN".
     */
    fun Authentication.hasRole(role: String): Boolean {
        return this.authorities.stream().anyMatch { it.authority == "ROLE_$role" }
    }

}