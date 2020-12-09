package no.id10022.pg6102.booking.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BookingRepository : JpaRepository<Booking, Long>{
    @Query("SELECT SUM(b.amount) FROM Booking b WHERE b.trip.id = :id AND b.cancelled = false")
    fun sumBookingAmountByTrip(@Param("id")tripId: Long): Long
}