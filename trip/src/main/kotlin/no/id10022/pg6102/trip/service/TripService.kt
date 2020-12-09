package no.id10022.pg6102.trip.service

import no.id10022.pg6102.trip.db.Trip
import no.id10022.pg6102.trip.db.TripRepository
import no.id10022.pg6102.utils.amqp.createTripRK
import no.id10022.pg6102.utils.amqp.deleteTripRK
import no.id10022.pg6102.utils.amqp.tripExchangeName
import no.id10022.pg6102.utils.rest.dto.TripDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import javax.persistence.EntityManager
import javax.persistence.TypedQuery

@Service
@Transactional
class TripService(
    private val em: EntityManager,
    private val repo: TripRepository,
    private val rabbit: RabbitTemplate
) {

    private val logger: Logger = LoggerFactory.getLogger(TripService::class.java)

    fun createTrip(dto: TripDto): Trip {
        return createTrip(
            title = dto.title!!,
            description = dto.description!!,
            location = dto.location!!,
            start = dto.start!!,
            end = dto.end!!,
            price = dto.price!!,
            capacity = dto.capacity!!
        )
    }

    fun createTrip(
        title: String,
        description: String,
        location: String,
        start: LocalDateTime,
        end: LocalDateTime,
        price: Int,
        capacity: Int
    ): Trip {
        // Create the Trip
        var trip = Trip(
            title = title,
            description = description,
            location = location,
            start = start,
            end = end,
            price = price,
            capacity = capacity
        )
        // Persist the Trip and get ID
        trip = repo.save(trip)
        // Publish AMQP message about new trip
        rabbit.convertAndSend(tripExchangeName, createTripRK, trip.id)
        // Log and return the Trip
        logger.info("Created Trip[id=${trip.id}]")
        return trip
    }

    fun deleteTrip(id: Long): Boolean {
        // Check if exists
        return if (repo.existsById(id)) {
            // Delete Trip
            repo.deleteById(id)
            // Publish message about delete Trip
            rabbit.convertAndSend(tripExchangeName, deleteTripRK, id)
            // Log and return
            logger.info("Deleted Trip[id=$id]")
            true
        } else {
            logger.info("Tried to delete non-existent Trip[id=$id]")
            false
        }
    }

    fun getNextPage(
        keysetId: Long?,
        keysetDate: LocalDateTime?,
        amount: Int
    ): List<Trip> {
        // Query should either have both or none of ID and Date
        if ((keysetId == null && keysetDate != null) || (keysetId != null && keysetDate == null)) {
            throw IllegalArgumentException("Need either both or none of keysetId and keysetDate")
        }
        // Check if first page
        val firstPage = (keysetId == null && keysetDate == null)
        // Create query
        val query: TypedQuery<Trip>
        if (firstPage) {
            query = em.createQuery(
                "SELECT t FROM Trip t WHERE t.start>?1 ORDER BY t.start ASC, t.id ASC",
                Trip::class.java
            )
            query.setParameter(1, LocalDateTime.now())
        } else {
            query = em.createQuery(
                "SELECT t FROM Trip t WHERE t.start>?2 OR (t.start=?2 AND t.id>?1) ORDER BY t.start ASC, t.id ASC",
                Trip::class.java
            )
            query.setParameter(1, keysetId)
            query.setParameter(2, keysetDate)
        }
        query.maxResults = amount
        return query.resultList
    }

}