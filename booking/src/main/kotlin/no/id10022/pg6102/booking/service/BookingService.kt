package no.id10022.pg6102.booking.service

import no.id10022.pg6102.booking.db.Booking
import no.id10022.pg6102.booking.db.BookingRepository
import no.id10022.pg6102.booking.db.Trip
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BookingService(
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
}