package no.id10022.pg6102.booking.service

import no.id10022.pg6102.booking.db.Booking
import no.id10022.pg6102.booking.db.BookingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.TypedQuery

@Service
@Transactional
class BookingService(
    private val em: EntityManager,
    private val repo: BookingRepository,
    private val userService: UserService,
    private val tripService: TripService
) {

    val logger: Logger = LoggerFactory.getLogger(BookingService::class.java)

    /**
     * Creates a Booking with the given amount
     * Fetches and locks the User and Trip to avoid overbooking
     * Throws IllegalArgumentException on errors
     * Returns the created Booking
     */
    fun createBooking(
        username: String,
        tripId: Long,
        amount: Int
    ): Booking {
        // Fetch locked User and Trip
        val user = userService.getUserByUsername(username, true)
            ?: throw IllegalArgumentException("User with username $username does not exist")
        val trip = tripService.getTripById(tripId, true)
            ?: throw IllegalArgumentException("Trip with ID $tripId does not exist")
        // Check if Trip is active
        if (trip.cancelled)
            throw IllegalArgumentException("Trip with ID $tripId is cancelled")
        // Check if Trip has capacity
        val totalCapacity = tripService.getTripCapacity(tripId)
            ?: throw IllegalArgumentException("Failed to retrieve capacity of Trip with ID $tripId")
        val bookingCount = repo.sumBookingAmountByTrip(tripId)
        val available = totalCapacity - bookingCount
        if (amount > available)
            throw IllegalArgumentException("Trip with ID $tripId has only $available slots left")
        // Persist, log and return
        val booking = repo.save(Booking(user = user, trip = trip, amount = amount))
        logger.info("Created Booking[id=${booking.id}]")
        return booking
    }

    /**
     * Sets a Booking as 'cancelled'
     * Does this directly in the database for better performance
     */
    fun cancelBooking(bookingId: Long): Boolean {
        return (repo.cancelBookingById(bookingId) == 1)
    }

    /**
     * Deletes a Booking entirely
     */
    fun deleteBooking(bookingId: Long) {
        repo.deleteById(bookingId)
    }

    /**
     * Gets a users Bookings sorted by ID ascending
     * Uses Keyset/Seek pagination based on only ID
     */
    fun getNextPage(
        username: String,
        keysetId: Long?,
        amount: Int
    ): List<Booking> {
        // Check if first page
        val firstPage = keysetId == null
        // Create query
        val query: TypedQuery<Booking>
        if (firstPage) {
            query = em.createQuery(
                "SELECT b FROM Booking b WHERE b.user.username = ?1 ORDER BY b.id ASC",
                Booking::class.java
            )
            query.setParameter(1, username)
        } else {
            query = em.createQuery(
                "SELECT b FROM Booking b WHERE b.user.username = ?1 AND b.id >?2 ORDER BY b.id ASC",
                Booking::class.java
            )
            query.setParameter(1, username)
            query.setParameter(2, keysetId)
        }
        query.maxResults = amount
        return query.resultList
    }

}