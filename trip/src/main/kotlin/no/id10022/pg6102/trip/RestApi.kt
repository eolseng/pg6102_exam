package no.id10022.pg6102.trip

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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


const val TRIPS_API_PATH = "$API_BASE_PATH/trips"

@Api(value = TRIPS_API_PATH, description = "Endpoint for managing Trips")
@RestController
@RequestMapping(
    path = [TRIPS_API_PATH],
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
        return RestResponseFactory.created(URI.create("$TRIPS_API_PATH/${trip.id}"))
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
        // Fetch Trip and convert to DTOs
        val dtos = service.getNextPage(keysetId, keysetDate, amount).map { it.toDto() }
        // Create PageDto
        val page = PageDto(list = dtos)
        // Check if not last page - will return a blank last page if match
        if (dtos.size == amount) {
            page.next = "$TRIPS_API_PATH?keysetId=${dtos.last().id}&keysetDate=${dtos.last().start}&amount=$amount"
        }
        // Return the page
        return RestResponseFactory.payload(200, page)
    }

    @GetMapping("/{id}")
    @ApiOperation("Retrieve a specific Trip by its ID")
    fun getTripById(
        @ApiParam("The ID of the Trip to retrieve")
        @PathVariable("id") id: Long
    ): ResponseEntity<WrappedResponse<Any>> {
        // Retrieve Trip from repository
        val trip = repo.findByIdOrNull(id)
            ?: return RestResponseFactory.notFound("Could not find Trip with ID $id")
        // Convert to DTO
        val dto = trip.toDto()
        // Send the DTO
        return RestResponseFactory.payload(200, dto)
    }

    @PatchMapping("/{id}")
    @ApiOperation("Update a specific Trip by its ID")
    fun patchTripById(
        @ApiParam("The ID of the Trip to update")
        @PathVariable("id") id: Long,
        @ApiParam("The partial patch to be applied")
        @RequestBody jsonPatch: String
    ): ResponseEntity<WrappedResponse<Void>> {
        // Get Trip to patch
        val trip = repo.findByIdOrNull(id)
            ?: return RestResponseFactory.userError("Trip with ID $id does not exist", 404)
        // Convert String to JsonNode
        val jsonNode = try {
            ObjectMapper().readValue(jsonPatch, JsonNode::class.java)
        } catch (e: Exception) {
            return RestResponseFactory.userError("Invalid JSON")
        }
        // Check if Patch node contains ID
        if (jsonNode.has("id"))
            return RestResponseFactory.userError("Cannot alter ID of existing Trip", 409)
        // Update the Trip - does not persist changes until all checks have passed
        // Title
        if (jsonNode.has("title")) {
            val node = jsonNode.get("title")
            when {
                node.isNull -> return RestResponseFactory.userError("Trip Title cannot be null", 409)
                node.asText().isEmpty() -> return RestResponseFactory.userError("Trip Title cannot be empty", 409)
                node.isTextual -> trip.title = node.asText()
                else -> return RestResponseFactory.userError("Invalid JSON on field 'title'")
            }
        }
        // Description
        if (jsonNode.has("description")) {
            val node = jsonNode.get("description")
            when {
                node.isNull -> return RestResponseFactory.userError("Trip Description cannot be null", 409)
                node.asText().isEmpty() -> return RestResponseFactory.userError("Trip Description cannot be empty", 409)
                node.isTextual -> trip.description = node.asText()
                else -> return RestResponseFactory.userError("Invalid JSON on field 'description'")
            }
        }
        // Location
        if (jsonNode.has("location")) {
            val node = jsonNode.get("location")
            when {
                node.isNull -> return RestResponseFactory.userError("Trip Location cannot be null", 409)
                node.asText().isEmpty() -> return RestResponseFactory.userError("Trip Location cannot be empty", 409)
                node.isTextual -> trip.location = node.asText()
                else -> return RestResponseFactory.userError("Invalid JSON on field 'location'")
            }
        }
        // Start
        if (jsonNode.has("start")) {
            val node = jsonNode.get("start")
            when {
                node.isNull -> return RestResponseFactory.userError("Trip Start cannot be null", 409)
                node.isTextual -> {
                    val dateTime = try {
                        LocalDateTime.parse(node.asText())
                    } catch (e: Exception) {
                        return RestResponseFactory.userError("Invalid JSON on field 'start'")
                    }
                    trip.start = dateTime
                }
                else -> return RestResponseFactory.userError("Invalid JSON on field 'start'")
            }
        }
        // End
        if (jsonNode.has("end")) {
            val node = jsonNode.get("end")
            when {
                node.isNull -> return RestResponseFactory.userError("Trip End cannot be null", 409)
                node.isTextual -> {
                    val dateTime = try {
                        LocalDateTime.parse(node.asText())
                    } catch (e: Exception) {
                        return RestResponseFactory.userError("Invalid JSON on field 'end'")
                    }
                    trip.end = dateTime
                }
                else -> return RestResponseFactory.userError("Invalid JSON on field 'end'")
            }
        }
        // Price
        if (jsonNode.has("price")) {
            val node = jsonNode.get("price")
            when {
                node.isNull -> return RestResponseFactory.userError("Trip Price cannot be null", 409)
                node.isInt -> {
                    val price = node.asInt()
                    if (price !in 0..Int.MAX_VALUE) {
                        return RestResponseFactory.userError(
                            "Trip Price must be in range of 1 to ${Int.MAX_VALUE}",
                            409
                        )
                    } else {
                        trip.price = price
                    }
                }
                else -> return RestResponseFactory.userError("Invalid JSON on field 'price'")
            }
        }
        // Capacity
        if (jsonNode.has("capacity")) {
            val node = jsonNode.get("capacity")
            when {
                node.isNull -> return RestResponseFactory.userError("Trip Capacity cannot be null", 409)
                node.isInt -> {
                    val capacity = node.asInt()
                    if (capacity !in 0..Int.MAX_VALUE) {
                        return RestResponseFactory.userError(
                            "Trip Capacity must be in range of 1 to ${Int.MAX_VALUE}",
                            409
                        )
                    } else {
                        trip.capacity = capacity
                    }
                }
                else -> return RestResponseFactory.userError("Invalid JSON on field 'capacity'")
            }
        }

        // Checked all fields - persist and return
        repo.save(trip)
        return ResponseEntity.status(204).build()
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