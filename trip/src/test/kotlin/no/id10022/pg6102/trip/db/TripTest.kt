package no.id10022.pg6102.trip.db

import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class TripTest {

    @Test
    fun testDuration() {

        val durationDays = 2
        val durationHours = 3
        val durationMinutes = 45

        val start = LocalDateTime.now()
        val end = start
            .plusDays(durationDays.toLong())
            .plusHours(durationHours.toLong())
            .plusMinutes(durationMinutes.toLong())

        val trip = Trip(
            start = start,
            end = end
        )

        assert(trip.duration["days"] == durationDays)
        assert(trip.duration["hours"] == durationHours)
        assert(trip.duration["minutes"] == durationMinutes)

    }

}