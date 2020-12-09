package no.id10022.pg6102.booking.service

import no.id10022.pg6102.booking.db.Trip
import no.id10022.pg6102.booking.db.TripRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker
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

    val tripUrl = (System.getenv("TRIP_SERVICE_NAME") ?: "localhost").trim()
    val tripPath = "/api/v1/trip"

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
     * Attempts to retrieve a Trip from the local repository.
     * If the ID does not exist locally it checks with the Trip Service,
     * If it does exist in the Trip Service - create and return the Trip,
     * Else return null.
     */
    fun getTripById(id: Long): Trip? {
        // Check if the Trip exists in local database
        return repo.findByIdOrNull(id)
        // Not in local database - check with Trip Service
            ?: if (verifyTrip(id)) {
                // Trip exists - create and return local copy
                createTrip((id))
            } else {
                // Trip does not exist - return null
                null
            }
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