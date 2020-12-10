package no.id10022.pg6102.booking

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.restassured.RestAssured
import io.restassured.http.ContentType
import no.id10022.pg6102.booking.db.BookingRepository
import no.id10022.pg6102.booking.db.Trip
import no.id10022.pg6102.booking.db.TripRepository
import no.id10022.pg6102.booking.db.UserRepository
import no.id10022.pg6102.booking.dto.BookingDto
import no.id10022.pg6102.booking.service.TripService
import no.id10022.pg6102.utils.rest.WrappedResponse
import no.id10022.pg6102.utils.rest.dto.TripDto
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [(BookingApplication::class)],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [(RestApiTest.Companion.Initializer::class)])
class RestApiTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var tripRepository: TripRepository

    @Autowired
    lateinit var tripService: TripService

    @BeforeEach
    @AfterEach
    fun setup() {
        // Setup RestAssured
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        RestAssured.basePath = BOOKINGS_API_PATH
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        // Clear repository
//        repo.deleteAll()
    }

    companion object {
        // Generates unique IDs
        private var idCounter = 1
        fun getId(): Int {
            return idCounter++
        }

        private lateinit var wiremockServer: WireMockServer

        //
        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServer = WireMockServer(
                WireMockConfiguration
                    .wireMockConfig()
                    .dynamicPort()
                    .notifier(ConsoleNotifier(true))
            )
            wiremockServer.start()


        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServer.stop()
        }
        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
                TestPropertyValues.of("services.address.trip=localhost:${wiremockServer.port()}")
                    .applyTo(configurableApplicationContext.environment)
            }
        }
    }

    /**
     * Utility function to make registration of Bookings easier
     * Returns the ID of the created Booking
     */
    fun registerBooking(
        username: String = "admin",
        password: String = "admin",
        tripId: Long,
        amount: Int
    ): Long {
        // Create DTO
        val dto = BookingDto(
            username = username,
            tripId = tripId,
            amount = amount
        )
        // Register the Booking and return the Booking ID
        val location =
            RestAssured.given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(dto)
                .post()
                .then().assertThat()
                .statusCode(201)
                .extract()
                .header("Location")
        return location.substringAfter("${RestAssured.basePath}/").toLong()
    }

    /**
     * Utility function to create a Trip in the local database
     */
    fun createTrip(): Long {
        return tripRepository.save(Trip(id = getId().toLong())).id!!
    }

    private fun mockTripRequest(id: Long) {
        val wrappedResponse = WrappedResponse(200, TripDto(id = id, capacity = 20)).validate()
        val json = ObjectMapper().writeValueAsString(wrappedResponse)
        wiremockServer.stubFor(
            get(urlEqualTo("/api/v1/trip/trips/$id"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(json)
                )
        )
        wiremockServer.stubFor(
            head(urlEqualTo("/api/v1/trip/trips/$id"))
                .willReturn(aResponse()
                    .withStatus(200))
        )
    }

    @Test
    fun `register Booking`() {
        mockTripRequest(780)
        tripService.getTripById(780, false)
    }


}