package no.id10022.pg6102.booking.service

import no.id10022.pg6102.booking.db.Booking
import no.id10022.pg6102.booking.db.BookingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.TypedQuery

const val MIN_BOOKING_AMOUNT = 1
const val MAX_BOOKING_AMOUNT = Int.MAX_VALUE

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
        // Validate amount
        if (!validateAmount(amount))
            throw IllegalArgumentException("Amount must be minimum $MIN_BOOKING_AMOUNT and maximum $MAX_BOOKING_AMOUNT")
        // Check if Trip is active
        if (trip.cancelled)
            throw IllegalArgumentException("Trip with ID $tripId is cancelled")
        // Check if Trip has capacity
        val availableCapacity = getAvailableCapacity(tripId)
        if (amount > availableCapacity)
            throw IllegalArgumentException("Trip with ID $tripId has only $availableCapacity slots left")
        // Persist, log and return
        val booking = repo.save(Booking(user = user, trip = trip, amount = amount))
        logger.info("Created Booking[id=${booking.id}]")
        return booking
    }

    /**
     * Updates the amount on a Booking
     * Locks Booking, Trip and User
     */
    fun updateAmount(bookingId: Long, amount: Int) {
        // Lock the Booking
        val booking = repo.findWithLock(bookingId)
            ?: throw IllegalArgumentException("Booking with id $bookingId does not exist")
        // Lock the User
        val username = booking.user.username!!
        userService.getUserByUsername(username, true)
            ?: throw IllegalArgumentException("User with username $username does not exist")
        // Lock the Trip
        val tripId = booking.trip.id!!
        tripService.getTripById(tripId, true)
            ?: throw IllegalArgumentException("Trip with ID $tripId does not exist")
        // Validate amount
        if (!validateAmount(amount))
            throw IllegalArgumentException("Amount must be minimum $MIN_BOOKING_AMOUNT and maximum $MAX_BOOKING_AMOUNT")
        // Check if Trip has capacity
        val availableCapacity = getAvailableCapacity(tripId)
        if (amount - booking.amount > availableCapacity)
            throw IllegalArgumentException("Trip with ID $tripId has only $availableCapacity slots left")
        // Update amount and persist
        booking.amount = amount
        repo.save(booking)
    }

    /**
     * Gets the available capacity on a Trip
     */
    fun getAvailableCapacity(tripId: Long): Long {
        val totalCapacity = tripService.getTripCapacity(tripId)
            ?: throw IllegalArgumentException("Failed to retrieve capacity of Trip with ID $tripId")
        val bookingCount = repo.sumBookingAmountByTrip(tripId) ?: 0
        return totalCapacity - bookingCount
    }

    /**
     * Sets a Booking as 'cancelled'
     * Does this directly in the database for better performance
     */
    fun cancelBooking(bookingId: Long): Boolean {
        return if (repo.cancelBookingById(bookingId) == 1) {
            logger.info("Cancelled Booking[id=$bookingId]")
            true
        } else {
            if (!repo.existsById(bookingId))
                logger.info("Failed to cancel Booking[id=$bookingId]. Reason: Does not exist")
            else
                logger.info("Failed to cancel Booking[id=$bookingId]. Reason: Unknown")
            false
        }
    }

    /**
     * Deletes a Booking entirely
     */
    fun deleteBooking(bookingId: Long) {
        if (repo.existsById(bookingId)) {
            logger.info("Deleted Booking[id=$bookingId]")
            repo.deleteById(bookingId)
        } else {
            logger.info("Failed to delete Booking[id=$bookingId]. Reason: Does not exist")
        }
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


    /**
     * Validates amount - should be in range of 1 to 100
     */
    private fun validateAmount(amount: Int): Boolean {
        return (amount in MIN_BOOKING_AMOUNT..MAX_BOOKING_AMOUNT)
    }
}