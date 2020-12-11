package no.id10022.pg6102.booking.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import javax.persistence.LockModeType

interface BookingRepository : JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    fun findWithLock(@Param("id") id: Long): Booking?

    /**
     * Sums the Amount column in not-cancelled Bookings on the given Trip
     */
    @Query("SELECT SUM(b.amount) FROM Booking b WHERE b.trip.id = :id AND b.cancelled = false")
    fun sumBookingAmountByTrip(@Param("id") tripId: Long): Long?

    /**
     * Updates the Booking to 'cancelled' directly in the database
     * Returns an Int of affected rows - should be 1 on success
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Booking b SET b.cancelled = true WHERE b.id = :id")
    fun cancelBookingById(@Param("id") id: Long): Int

}