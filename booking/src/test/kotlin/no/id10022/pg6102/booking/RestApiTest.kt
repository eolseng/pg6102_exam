package no.id10022.pg6102.booking

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.restassured.RestAssured
import io.restassured.http.ContentType
import no.id10022.pg6102.booking.db.BookingRepository
import no.id10022.pg6102.booking.db.TripRepository
import no.id10022.pg6102.booking.db.UserRepository
import no.id10022.pg6102.booking.dto.BookingDto
import no.id10022.pg6102.booking.dto.Command
import no.id10022.pg6102.booking.dto.PatchBookingDto
import no.id10022.pg6102.utils.rest.WrappedResponse
import no.id10022.pg6102.utils.rest.dto.TripDto
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [(BookingApplication::class)],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [(RestApiTest.Companion.Initializer::class)])
internal class RestApiTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var tripRepository: TripRepository

    @BeforeEach
    @AfterEach
    fun setup() {
        // Setup RestAssured
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        RestAssured.basePath = BOOKINGS_API_PATH
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        // Clear repository
        bookingRepository.deleteAll()
        userRepository.deleteAll()
        tripRepository.deleteAll()
    }

    companion object {
        // Generates unique IDs
        private var idCounter = 1
        fun getId(): Int {
            return idCounter++
        }

        private lateinit var wiremockServer: WireMockServer

        // Kotlin wrapper class for GenericContainer
        class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

        // Starts a Redis Docker container
        @Container
        @JvmField
        val redis: KGenericContainer = KGenericContainer("redis:latest").withExposedPorts(6379)

        // Starts a RabbitMQ Docker container
        @Container
        @JvmField
        val rabbitMQ: KGenericContainer = KGenericContainer("rabbitmq:3").withExposedPorts(5672)

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
                // Route requests for Trip Service to WireMock
                TestPropertyValues.of("services.address.trip=localhost:${wiremockServer.port()}")
                    .applyTo(configurableApplicationContext.environment)
                // Setup Redis and RabbitMQ
                TestPropertyValues.of(
                    "spring.redis.host=${redis.containerIpAddress}",
                    "spring.redis.port=${redis.getMappedPort(6379)}",
                    "spring.rabbitmq.host=${rabbitMQ.containerIpAddress}",
                    "spring.rabbitmq.port=${rabbitMQ.getMappedPort(5672)}"
                ).applyTo(configurableApplicationContext.environment)
            }
        }
    }

    /**
     * Utility function to make registration of Bookings easier
     * Defaults to the 'user' User and with 1 in amount
     * Returns the ID of the created Booking
     */
    fun registerBooking(
        tripId: Long? = null,
        username: String = "user",
        password: String = "user",
        amount: Int = 1,
        tripCapacity: Int = 20,
        mock: Boolean = false
    ): Long {
        val trip = tripId ?: getId().toLong()
        if (mock) {
            // Mock Trip if no ID was supplied
            mockTripRequest(trip, tripCapacity)
        }
        // Create DTO
        val dto = BookingDto(
            username = username,
            tripId = trip,
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
     * Stubs both GET and HEAD request for the Single Trip endpoint
     */
    private fun mockTripRequest(id: Long, capacity: Int = 20) {
        val tripDto = TripDto(id = id, capacity = capacity)
        val wrappedResponse = WrappedResponse(200, tripDto).validate()
        val json = ObjectMapper().writeValueAsString(wrappedResponse)
        wiremockServer.stubFor(
            get(urlEqualTo("/api/v1/trip/trips/$id"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(json)
                )
        )
        wiremockServer.stubFor(
            head(urlEqualTo("/api/v1/trip/trips/$id"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                )
        )
    }

    @Test
    fun `register booking`() {
        registerBooking(mock = true)
        assertTrue(bookingRepository.count() == 1L)
        registerBooking(username = "user", password = "user", amount = 5, tripCapacity = 5, mock = true)
        assertTrue(bookingRepository.count() == 2L)
        registerBooking(username = "extra", password = "extra", amount = 10, tripCapacity = 10, mock = true)
        assertTrue(bookingRepository.count() == 3L)
    }

    @Test
    fun `register booking - overbook trip`() {
        val tripId = getId().toLong()
        val tripCapacity = 5
        registerBooking(tripId = tripId, amount = tripCapacity, tripCapacity = tripCapacity, mock = true)

        val dto = BookingDto(
            username = "user",
            tripId = tripId,
            amount = 1
        )
        RestAssured.given()
            .auth().basic("user", "user")
            .contentType(ContentType.JSON)
            .body(dto)
            .post()
            .then().assertThat()
            .statusCode(400)
    }

    @Test
    fun `retrieve Booking by id`() {
        val amount = 5
        val username = "user"
        val password = "user"
        val bookingId = registerBooking(username = username, password = password, amount = amount, mock = true)

        // Unauthenticated
        RestAssured.given()
            .accept(ContentType.JSON)
            .get("/$bookingId")
            .then().assertThat()
            .statusCode(401)

        // Wrong user
        RestAssured.given()
            .accept(ContentType.JSON)
            .auth().basic("extra", "extra")
            .get("/$bookingId")
            .then().assertThat()
            .statusCode(403)

        // Correct user
        RestAssured.given()
            .accept(ContentType.JSON)
            .auth().basic(username, password)
            .get("/$bookingId")
            .then().assertThat()
            .statusCode(200)
            .body("data.id", equalTo(bookingId.toInt()))
            .body("data.username", equalTo(username))
            .body("data.tripId", notNullValue())
            .body("data.amount", equalTo(amount))
            .body("data.cancelled", equalTo(false))

        // As admin
        RestAssured.given()
            .accept(ContentType.JSON)
            .auth().basic("admin", "admin")
            .get("/$bookingId")
            .then().assertThat()
            .statusCode(200)
            .body("data.id", equalTo(bookingId.toInt()))
            .body("data.username", equalTo(username))
            .body("data.tripId", notNullValue())
            .body("data.amount", equalTo(amount))
            .body("data.cancelled", equalTo(false))

    }

    @Test
    fun `get all Bookings - single page`() {

        val amount = 10
        // Register Bookings
        for (x in 0 until amount) {
            registerBooking(username = "user", password = "user", mock = true)
        }
        // Verify with repository
        assertEquals(amount, bookingRepository.count().toInt())

        RestAssured.given()
            .accept(ContentType.JSON)
            .auth().basic("user", "user")
            .get("?amount=${amount + 1}")
            .then().assertThat()
            .statusCode(200)
            .body("data.list.size()", equalTo(amount))
            .body("data.next", nullValue())

    }

    @Test
    fun `patch booking - amount valid`() {

        val originalAmount = 5
        val id = registerBooking(amount = originalAmount, mock = true)
        val newAmount = originalAmount - 1
        val dto = PatchBookingDto(Command.UPDATE_AMOUNT, newAmount = newAmount)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().basic("user", "user")
            .body(dto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(204)

        val booking = bookingRepository.findById(id).get()
        assertEquals(newAmount, booking.amount)

    }

    @Test
    fun `patch booking - amount overbook`() {

        val tripCapacity = 5
        val id = registerBooking(amount = tripCapacity, tripCapacity = tripCapacity, mock = true)
        val newAmount = tripCapacity + 1
        val dto = PatchBookingDto(Command.UPDATE_AMOUNT, newAmount = newAmount)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().basic("user", "user")
            .body(dto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(400)

        val booking = bookingRepository.findById(id).get()
        assertEquals(tripCapacity, booking.amount)

    }

    @Test
    fun `patch booking - cancel`() {

        val id = registerBooking(mock = true)
        val dto = PatchBookingDto(Command.CANCEL)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().basic("user", "user")
            .body(dto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(204)

        val booking = bookingRepository.findById(id).get()
        assertTrue(booking.cancelled)

    }

    @Test
    fun `patch booking - security`() {

        val id = registerBooking(mock = true)
        val dto = PatchBookingDto(Command.CANCEL)

        // Unauthorized
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(dto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(401)

        // As valid user
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().basic("user", "user")
            .body(dto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(204)

        // As invalid user
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().basic("extra", "extra")
            .body(dto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(403)

        // As ADMIN
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth().basic("admin", "admin")
            .body(dto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(204)

    }

}