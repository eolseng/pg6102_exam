package no.id10022.pg6102.booking.service

import no.id10022.pg6102.booking.db.Trip
import no.id10022.pg6102.booking.db.TripRepository
import no.id10022.pg6102.utils.rest.WrappedResponse
import no.id10022.pg6102.utils.rest.dto.TripDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

@Service
@Transactional
class TripService(
    private val repo: TripRepository,
    private val client: RestTemplate,
    @Qualifier("tripCircuitBreaker")
    private val cb: CircuitBreaker
) {

    val logger: Logger = LoggerFactory.getLogger(TripService::class.java)

    @Value("\${services.address.trip}")
    private lateinit var tripUrl: String
    val tripPath = "/api/v1/trip"

    /**
     * Fetches a Trip DTO from the Trip Service
     */
    fun fetchTrip(id: Long): TripDto? {
        val uri = URI("http://$tripUrl$tripPath/trips/$id")
        return cb.run(
            {
                // Fetch a Trip from the Trip Service and retrieve the TripDto
                client.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<WrappedResponse<TripDto>>() {})
                    .body
                    ?.data
            },
            { err ->
                // Failed to fetch Trip from Trip Service
                logger.error("Failed to retrieve data from Trip Service: ${err.message}")
                null
            }
        )
    }

    /**
     * Fetches a Trip from the Trip Service to get the Trip Capacity
     */
    fun getTripCapacity(id: Long): Int? {
        return fetchTrip(id)?.capacity
    }

    /**
     * Creates a local copy of a Trip.
     * Usually gets called from an AMQP triggered event.
     */
    fun createTrip(id: Long): Trip {
        val trip = repo.save(Trip(id = id))
        logger.info("Created Trip[id=$id")
        return trip
    }

    /**
     * Cancels the local copy of the Trip and all Bookings
     * Usually gets called from an AMQP triggered event
     */
    fun cancelTrip(id: Long): Boolean {
        // Get the Trip
        val trip = getTripById(id, true) ?: return false
        // Mark Trip as cancelled
        trip.cancelled = true
        // Mark all Bookings as cancelled
        trip.bookings.forEach { it.cancelled = true }
        // Persist, log and return
        repo.save(trip)
        logger.info("Cancelled Trip[id=$id]")
        trip.bookings.forEach { logger.info("Cancelled Booking[id=${it.id}]. Reason: Cancelled Trip[id=$id]") }
        return true
    }

    /**
     * Attempts to retrieve a Trip from the local repository.
     * If the ID does not exist locally it checks with the Trip Service,
     * If it does exist in the Trip Service - create and return the Trip,
     * Else return null.
     */
    fun getTripById(id: Long, locked: Boolean): Trip? {
        // Check if Trip exists in local database
        if (!repo.existsById(id)) {
            // Create new Trip if Trip Service confirms Trip with given ID
            if (verifyTrip(id))
                createTrip(id)
            // Trip does not exist
            else
                return null
        }
        // Return the Trip
        return if (locked) repo.findWithLock(id) else repo.findByIdOrNull(id)
    }

    /**
     * Verifies the existence of a Trip with the Trip Service.
     * Used as a backup in case it has not been created with an AMQP-message.
     */
    private fun verifyTrip(id: Long): Boolean {
        val uri = URI("http://$tripUrl$tripPath/trips/$id")
        return cb.run(
            {
                try {
                    // Use HEAD request to check with Blueprint Service if Blueprint exists
                    val res = client.exchange(uri, HttpMethod.HEAD, null, String::class.java)
                    if (!res.statusCode.is2xxSuccessful) {
                        logger.info("Verified with Trip Service: Trip[id=$id]")
                        true
                    } else {
                        false
                    }
                } catch (err: HttpClientErrorException.NotFound) {
                    // Catch 404 as an expected error
                    false
                }
            },
            { err ->
                // Client did not return 2xx or 404
                logger.error("Failed to retrieve data from Trip Service: ${err.message}")
                false
            }
        )
    }

}