package no.id10022.pg6102.trip.db

import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class TripTest {

    @Test
    fun testDuration() {

        val durationDays = 2L
        val durationHours = 3L
        val durationMinutes = 45L

        val start = LocalDateTime.now()
        val end = start.plusDays(durationDays).plusHours(durationHours).plusMinutes(durationMinutes)

        val trip = Trip(
            start = start,
            end = end
        )

        assert(trip.duration["Days"] == durationDays)
        assert(trip.duration["Hours"] == durationHours)
        assert(trip.duration["Minutes"] == durationMinutes)

    }

}