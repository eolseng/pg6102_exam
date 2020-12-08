package no.id10022.pg6102.trip.db

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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

        assert(trip.duration[ChronoUnit.DAYS] == durationDays)
        assert(trip.duration[ChronoUnit.HOURS] == durationHours)
        assert(trip.duration[ChronoUnit.MINUTES] == durationMinutes)

    }

}