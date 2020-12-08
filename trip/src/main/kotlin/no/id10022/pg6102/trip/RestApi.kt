package no.id10022.pg6102.trip

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.id10022.pg6102.trip.db.TripRepository
import no.id10022.pg6102.trip.db.toDto
import no.id10022.pg6102.trip.dto.TripDto
import no.id10022.pg6102.trip.dto.isValidForRegistration
import no.id10022.pg6102.trip.service.TripService
import no.id10022.pg6102.utils.rest.PageDto
import no.id10022.pg6102.utils.rest.RestResponseFactory
import no.id10022.pg6102.utils.rest.WrappedResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.LocalDateTime

const val API_BASE_PATH = "/api/v1/blueprint"
const val TRIPS_PATH = "$API_BASE_PATH/trips"

@Api(value = TRIPS_PATH, description = "Endpoint for managing Trips")
@RestController
@RequestMapping(
    path = [TRIPS_PATH],
    produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
class RestApi(
    private val repo: TripRepository,
    private val service: TripService
) {

    @PostMapping
    @ApiOperation("Create a new Trip")
    fun createTrip(
        @ApiParam("Data for the new Trip")
        @RequestBody dto: TripDto
    ): ResponseEntity<WrappedResponse<Void>> {
        // Validate the DTO
        if (!dto.isValidForRegistration())
            return RestResponseFactory.userError("Invalid registration data")
        // Persist the Trip
        val trip = service.createTrip(dto)
        // Return path to the created Trip
        return RestResponseFactory.created(URI.create("$TRIPS_PATH/${trip.id}"))
    }

    @GetMapping
    @ApiOperation("Retrieve all upcoming Trips, sorted by Start")
    fun getAllTrips(
        @RequestParam("keysetId", required = false)
        keysetId: Long?,
        @RequestParam("keysetDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        keysetDate: LocalDateTime?,
        @RequestParam("amount", required = false)
        amountParam: Int?
    ): ResponseEntity<WrappedResponse<PageDto<TripDto>>> {
        // Set amount if not supplied
        val amount = amountParam ?: 10
        // Verify amount is in range
        if (amount !in 1..1000)
            return RestResponseFactory.userError("Amount must be between in the range of 1 to 1000")
        // Create page dto
        val page = PageDto<TripDto>()
        // Fetch Trip and convert to DTOs
        val dtos = service.getNextPage(keysetId, keysetDate, amount).map { it.toDto() }
        page.list = dtos
        // Check if not last page - will return a blank last page if match
        if (dtos.size == amount) {
            page.next = "$TRIPS_PATH?keysetId=${dtos.last().id}&keysetDate=${dtos.last().start}&amount=$amount"
        }
        // Return the page
        return RestResponseFactory.payload(200, page)
    }

    @GetMapping("/{id}")
    @ApiOperation("Retrieve a specific Trip by its ID")
    fun getTripById(
        @ApiParam("The ID of the Trip to retrieve")
        @PathVariable("id") pathId: String
    ): ResponseEntity<WrappedResponse<Any>> {
        // Convert pathId to Int value
        val id = pathId.toLongOrNull()
            ?: return RestResponseFactory.userError("ID must be a number")
        // Retrieve Trip from repository
        val trip = repo.findByIdOrNull(id)
            ?: return RestResponseFactory.notFound("Could not find Trip with ID $id")
        // Convert to DTO
        val dto = trip.toDto()
        // Send the DTO
        return RestResponseFactory.payload(200, dto)
    }

    @DeleteMapping("/{id}")
    @ApiOperation("Delete a specific Trip by its ID")
    fun deleteTripById(
        @ApiParam("The ID of the Trip to delete")
        @PathVariable("id") id: Long
    ): ResponseEntity<WrappedResponse<Void>> {
        return if (!service.deleteTrip(id)) {
            RestResponseFactory.userError(httpStatusCode = 404, message = "Trip with ID $id does not exist")
        } else {
            RestResponseFactory.noPayload(204)
        }
    }
}