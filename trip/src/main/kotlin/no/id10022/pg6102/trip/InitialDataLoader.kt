package no.id10022.pg6102.trip

import no.id10022.pg6102.trip.db.TripRepository
import no.id10022.pg6102.trip.service.TripService
import no.id10022.pg6102.utils.rest.dto.TripDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime


@Component
@Profile("!test")
class InitialDataLoader(
    private val repo: TripRepository,
    private val service: TripService
) : CommandLineRunner {

    val logger : Logger = LoggerFactory.getLogger(InitialDataLoader::class.java)

    override fun run(vararg args: String?) {
        if (repo.count() == 0L) {
            createTrips()
        }
    }

    private fun createTrips() {
        logger.info("Creating default trips")
        val trips = listOf(
            TripDto(
                title = "The Great Trip to Vardø",
                description = "Join the crew on this amazing trip of a lifetime to Vardø - the heart of the world!",
                location = "Vardø",
                start = LocalDateTime.of(2021, 2, 12, 8, 0),
                end = LocalDateTime.of(2021, 2, 12, 10, 0),
                price = 2000,
                capacity = 5
            ),
            TripDto(
                title = "Skiing with friends",
                description = "The super steep slopes of Gardermoen will rock your world!",
                location = "Gardermoen Airport",
                start = LocalDateTime.of(2021, 7, 1, 20, 0),
                end = LocalDateTime.of(2021, 7, 1, 23, 30),
                price = 200,
                capacity = 20
            ),
            TripDto(
                title = "Slow Dancing",
                description = "We will take you to the perfect palce for slow dancing - Rockefeller!",
                location = "Rockefeller",
                start = LocalDateTime.of(2021, 1, 18, 18, 0),
                end = LocalDateTime.of(2021, 1, 18, 22, 0),
                price = 500,
                capacity = 50
            ),
            TripDto(
                title = "Deep Dives & High Cigars",
                description = "Want to see the worlds strangest fish? Or are you afraid of the deep? After the dive we will take a SpaceX rocket to shore.",
                location = "Marinara Trench",
                start = LocalDateTime.of(2021, 2, 12, 12, 0),
                end = LocalDateTime.of(2021, 2, 12, 22, 0),
                price = 25000,
                capacity = 20
            ),
            TripDto(
                title = "Meet Your Maker: One-on-One with Tyson",
                description = "Ever wondered who made you? Now is you time for a one-on-one fight with Mike Tyson!",
                location = "Spectrum",
                start = LocalDateTime.of(2021, 8, 23, 18, 0),
                end = LocalDateTime.of(2021, 8, 23, 18, 5),
                price = 0,
                capacity = 1
            )
        )
        for (trip in trips) {
            service.createTrip(trip)
        }
        logger.info("Done creating default trips")
    }
}