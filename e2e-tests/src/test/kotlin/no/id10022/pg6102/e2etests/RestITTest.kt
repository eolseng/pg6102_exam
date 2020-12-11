package no.id10022.pg6102.e2etests

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import no.id10022.pg6102.auth.dto.AuthDto
import no.id10022.pg6102.booking.dto.BookingDto
import no.id10022.pg6102.booking.dto.Command
import no.id10022.pg6102.booking.dto.PatchBookingDto
import no.id10022.pg6102.utils.rest.dto.TripDto
import org.awaitility.Awaitility
import org.awaitility.pollinterval.FibonacciPollInterval.fibonacci
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.random.Random

const val AUTH_API_PATH = "/api/v1/auth"
const val TRIP_API_PATH = "/api/v1/trip"
const val BOOKING_API_PATH = "/api/v1/booking"

@Testcontainers
class RestIT {

    companion object {
        init {
            // Configure RestAssured
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
            RestAssured.port = 80
        }

        // Generates unique IDs
        private var idCounter = 1
        fun getId(): Int {
            return idCounter++
        }

        // Kotlin wrapper class for DockerComposeContainer
        class KDockerComposeContainer(id: String, path: File) :
            DockerComposeContainer<KDockerComposeContainer>(id, path)

        @Container
        @JvmField
        val env: KDockerComposeContainer = KDockerComposeContainer("travel-agency", File("../docker-compose.yml"))
            .withExposedService(
                "discovery",
                8500,
                Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240))
            )
            .withLogConsumer("gateway") { print("[${"GATEWAY".padEnd(7)}] ${it.utf8String}") }
            .withLogConsumer("auth") { print("[${"AUTH".padEnd(7)}] ${it.utf8String}") }
            .withLogConsumer("trip_0") { print("[${"TRIP_0".padEnd(7)}] ${it.utf8String}") }
            .withLogConsumer("trip_1") { print("[${"TRIP_1".padEnd(7)}] ${it.utf8String}") }
            .withLogConsumer("booking") { print("[${"BOOKING".padEnd(7)}] ${it.utf8String}") }

        @BeforeAll
        @JvmStatic
        fun waitForServers() {
            Awaitility.await().atMost(240, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(20))
                .ignoreExceptions()
                .until {
                    RestAssured.given().baseUri("http://${env.getServiceHost("discovery", 8500)}")
                        .port(env.getServicePort("discovery", 8500))
                        .get("/v1/agent/services")
                        .then().assertThat()
                        .body("size()", equalTo(5))
                    true
                }
        }
    }

    /**
     * Utility function to get a registered Session Cookie.
     * Generates a unique UserDto and registers it
     * @returns the authenticated 'SESSION' cookie
     */
    private fun getRegisteredSessionCookie(): String {
        val authDto = AuthDto(username = "test_username_${getId()}", password = "test_password")
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(authDto)
            .put("$AUTH_API_PATH/signup/${authDto.username}")
            .then().assertThat()
            .statusCode(201)
            .header("Location", containsString("$AUTH_API_PATH/user"))
            .header("Set-Cookie", containsString("SESSION"))
            .cookie("SESSION")
            .extract().cookie("SESSION")
    }

    /**
     * Creates a Admin Session
     * @returns the authenticated Admin Session Cookie
     */
    private fun getAdminSession(): String {
        val authDto = AuthDto("admin", "admin")
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(authDto)
            .post("$AUTH_API_PATH/login")
            .then().assertThat()
            .statusCode(204)
            .cookie("SESSION")
            .extract().cookie("SESSION")
    }

    /**
     * Fetches the username of the Session from the Auth Service
     * @returns the username
     */
    private fun getUsername(session: String): String {
        return RestAssured.given()
            .cookie("SESSION", session)
            .get("$AUTH_API_PATH/user")
            .then().assertThat()
            .statusCode(200)
            .extract()
            .body()
            .jsonPath()
            .getString("data.username")
    }

    /**
     * Utility function to register a Trip
     * Requires a valid Admin Session
     * @return the ID of the created Trip
     */
    private fun registerTrip(
        startDelay: Long = 0,
        durationInMinutes: Long = Random.nextLong(30, 14440),
        price: Int = Random.nextInt(25, 500),
        capacity: Int = Random.nextInt(1, 20)
    ): Long {
        val session = getAdminSession()
        // Create the DTO
        val uniqueIdentifier = getId()
        val start = LocalDateTime.now().plusDays(1 + startDelay)
        val dto = TripDto(
            title = "test_title_$uniqueIdentifier",
            description = "test_description_$uniqueIdentifier",
            location = "test_location_$uniqueIdentifier",
            start = start,
            end = start.plusMinutes(durationInMinutes),
            price = price,
            capacity = capacity
        )
        // Register the Trip and extract the Location Header
        val redirect =
            RestAssured.given()
                .cookie("SESSION", session)
                .contentType(ContentType.JSON)
                .body(dto)
                .post("$TRIP_API_PATH/trips")
                .then().assertThat()
                .statusCode(201)
                .extract()
                .header("Location")
        // Extract the ID from the Location Header and return it
        return redirect.substringAfter("$TRIP_API_PATH/trips/").toLong()
    }

    /**
     * Registers a Booking on the given Trip
     * @return the ID of the Booking
     */
    private fun registerBooking(
        session: String,
        tripId: Long,
        amount: Int = 1
    ): Long {
        // Get the username of the session
        val username = getUsername(session)
        // Create DTO
        val dto = BookingDto(
            username = username,
            tripId = tripId,
            amount = amount
        )
        // Register the Booking and return the Booking ID
        val location =
            RestAssured.given()
                .contentType(ContentType.JSON)
                .cookie("SESSION", session)
                .body(dto)
                .post("$BOOKING_API_PATH/bookings")
                .then().assertThat()
                .statusCode(201)
                .extract()
                .header("Location")
        return location.substringAfter("$BOOKING_API_PATH/bookings/").toLong()
    }

    @Test
    fun `auth - user-endpoint - unauthorized`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                // Unauthorized
                RestAssured.given()
                    .get("$AUTH_API_PATH/user")
                    .then().assertThat()
                    .statusCode(401)
                true
            }
    }

    @Test
    fun `auth - user-endpoint - authenticated`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                // Authenticated
                val session = getRegisteredSessionCookie()
                RestAssured.given()
                    .cookie("SESSION", session)
                    .get("$AUTH_API_PATH/user")
                    .then().assertThat()
                    .statusCode(200)
                true
            }
    }

    @Test
    fun `auth - register`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                getRegisteredSessionCookie()
                true
            }
    }

    @Test
    fun `auth - logout`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val session = getRegisteredSessionCookie()
                RestAssured.given()
                    .cookie("SESSION", session)
                    .post("$AUTH_API_PATH/logout")
                    .then().assertThat()
                    .statusCode(204)
                // Verify session is destroyed
                RestAssured.given()
                    .cookie("SESSION", session)
                    .get("$AUTH_API_PATH/user")
                    .then().assertThat()
                    .statusCode(401)
                true
            }
    }

    @Test
    fun `trip - register trip`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val adminSession = getAdminSession()
                registerTrip()
                true
            }
    }

    @Test
    fun `trip - get single trip`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val adminSession = getAdminSession()
                val price = 789
                val capacity = 88
                val id = registerTrip(price = price, capacity = capacity)
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .get("$TRIP_API_PATH/trips/$id")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.id", equalTo(id.toInt()))
                    .body("data.title", notNullValue())
                    .body("data.description", notNullValue())
                    .body("data.location", notNullValue())
                    .body("data.duration.days", notNullValue())
                    .body("data.duration.hours", notNullValue())
                    .body("data.duration.minutes", notNullValue())
                    .body("data.price", equalTo(price))
                    .body("data.capacity", equalTo(capacity))
                true
            }
    }

    @Test
    fun `trip - get multiple trips`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                // Register multiple trips - low number to reduce test times
                val half = 2
                val amount = half * 2
                for (x in 0 until amount) {
                    registerTrip()
                }
                // Get all in a single page
                // Asking for huge page size in case of other tests registering Trips
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .get("$TRIP_API_PATH/trips?amount=${amount * 25}")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.list.size()", greaterThan(amount - 1))
                    .body("data.next", nullValue())

                // Get less than all registered pages
                val res = RestAssured.given()
                    .accept(ContentType.JSON)
                    .get("$TRIP_API_PATH/trips?amount=$half")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.list.size()", equalTo(half))
                    .body("data.next", anything())
                    .extract()
                    .response()
                val nextLink = res.getNextLink()
                // Get the next page
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .get(nextLink)
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.list.size()", equalTo(half))
                true
            }
    }

    @Test
    fun `trip - patch trip`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val adminSession = getAdminSession()
                val id = registerTrip()

                // Patch the Trip
                val durationDays = 1
                val durationHours = 12
                val durationMinutes = 30
                val newTitle = "new_title"
                val newDescription = "new_description"
                val newLocation = "new_location"
                val newStart = LocalDateTime.now().plusYears(1)
                val newEnd = newStart.plusDays(durationDays.toLong()).plusHours(durationHours.toLong())
                    .plusMinutes(durationMinutes.toLong())
                val newPrice = 123
                val newCapacity = 123

                // Create patch DTO
                val patchDto = ObjectMapper().createObjectNode()
                    .put("title", newTitle)
                    .put("description", newDescription)
                    .put("location", newLocation)
                    .put("start", newStart.toString())
                    .put("end", newEnd.toString())
                    .put("price", newPrice)
                    .put("capacity", newCapacity)

                // Apply patch
                RestAssured.given()
                    .cookie("SESSION", adminSession)
                    .contentType(ContentType.JSON)
                    .body(patchDto)
                    .patch("$TRIP_API_PATH/trips/$id")
                    .then().assertThat()
                    .statusCode(204)

                // Verify the update
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .get("$TRIP_API_PATH/trips/$id")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.id", equalTo(id.toInt()))
                    .body("data.title", equalTo(newTitle))
                    .body("data.description", equalTo(newDescription))
                    .body("data.location", equalTo(newLocation))
                    .body("data.duration.days", equalTo(durationDays))
                    .body("data.duration.hours", equalTo(durationHours))
                    .body("data.duration.minutes", equalTo(durationMinutes))
                    .body("data.price", equalTo(newPrice))
                    .body("data.capacity", equalTo(newCapacity))
                true
            }
    }

    @Test
    fun `booking - register booking`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                // Register a Trip
                val tripId = registerTrip()
                // Create user and register Booking on trip
                val session = getRegisteredSessionCookie()
                registerBooking(session, tripId)
                true
            }
    }

    @Test
    fun `booking - overbook trip`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val capacity = 5
                // Register a Trip
                val tripId = registerTrip(capacity = capacity)
                // Overbook Trip
                val session = getRegisteredSessionCookie()
                // Get the username of the session
                val username = getUsername(session)
                // Create overbooking Booking
                val dto = BookingDto(
                    username = username,
                    tripId = tripId,
                    amount = capacity + 1
                )
                // Register the Booking and return the Booking ID
                RestAssured.given()
                    .contentType(ContentType.JSON)
                    .cookie("SESSION", session)
                    .body(dto)
                    .post("$BOOKING_API_PATH/bookings")
                    .then().assertThat()
                    .statusCode(400)
                true
            }
    }

    @Test
    fun `booking - get single booking - unauthenticated`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {

                val session = getRegisteredSessionCookie()
                val tripId = registerTrip()
                val bookingId = registerBooking(session, tripId)

                // Unauthenticated
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .get("$BOOKING_API_PATH/bookings/$bookingId")
                    .then().assertThat()
                    .statusCode(401)
                true
            }
    }

    @Test
    fun `booking - get single booking - wrong user`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {

                val session = getRegisteredSessionCookie()
                val tripId = registerTrip()
                val bookingId = registerBooking(session, tripId)

                // Wrong user
                val wrongSession = getRegisteredSessionCookie()
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .cookie("SESSION", wrongSession)
                    .get("$BOOKING_API_PATH/bookings/$bookingId")
                    .then().assertThat()
                    .statusCode(403)
                true
            }
    }

    @Test
    fun `booking - get single booking - correct user`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {

                val session = getRegisteredSessionCookie()
                val username = getUsername(session)
                val tripId = registerTrip()
                val bookingId = registerBooking(session, tripId)

                // Correct user
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .cookie("SESSION", session)
                    .get("$BOOKING_API_PATH/bookings/$bookingId")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.id", equalTo(bookingId.toInt()))
                    .body("data.username", equalTo(username))
                    .body("data.tripId", notNullValue())
                    .body("data.amount", notNullValue())
                    .body("data.cancelled", equalTo(false))
                true
            }
    }

    @Test
    fun `booking - get single booking - as admin`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {

                val session = getRegisteredSessionCookie()
                val username = getUsername(session)
                val tripId = registerTrip()
                val bookingId = registerBooking(session, tripId)

                // As admin
                val adminSession = getAdminSession()
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .cookie("SESSION", adminSession)
                    .get("$BOOKING_API_PATH/bookings/$bookingId")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.id", equalTo(bookingId.toInt()))
                    .body("data.username", equalTo(username))
                    .body("data.tripId", notNullValue())
                    .body("data.amount", notNullValue())
                    .body("data.cancelled", equalTo(false))
                true
            }
    }

    @Test
    fun `booking - get multiple bookings - single page`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val session = getRegisteredSessionCookie()
                val tripId = registerTrip()
                // Register Bookings
                val amount = 2
                for (x in 0 until amount) {
                    registerBooking(session, tripId)
                }
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .cookie("SESSION", session)
                    .get("$BOOKING_API_PATH/bookings?amount=${amount + 1}")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.list.size()", greaterThan(amount - 1))
                    .body("data.next", nullValue())
                true
            }
    }

    @Test
    fun `booking - get multiple bookings - multiple pages`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val session = getRegisteredSessionCookie()
                val tripId = registerTrip()
                // Register Bookings
                val half = 2
                val amount = half * 2
                for (x in 0 until amount) {
                    registerBooking(session, tripId)
                }
                // Get a full page of 'half' size
                val res = RestAssured.given()
                    .accept(ContentType.JSON)
                    .cookie("SESSION", session)
                    .get("$BOOKING_API_PATH/bookings?amount=${half}")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.list.size()", equalTo(half))
                    .body("data.next", anything())
                    .extract()
                    .response()
                val nextLink = res.getNextLink()
                // Get the next page
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .cookie("SESSION", session)
                    .get(nextLink)
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.list.size()", equalTo(half))
                true
            }
    }

    @Test
    fun `booking - patch amount`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val tripId = registerTrip()
                val session = getRegisteredSessionCookie()
                val originalAmount = 5
                val bookingId = registerBooking(session, tripId, originalAmount)

                // Patch the amount on the booking
                val newAmount = originalAmount - 1
                val dto = PatchBookingDto(Command.UPDATE_AMOUNT, newAmount = newAmount)
                RestAssured.given()
                    .contentType(ContentType.JSON)
                    .cookie("SESSION", session)
                    .body(dto)
                    .patch("$BOOKING_API_PATH/bookings/$bookingId")
                    .then().assertThat()
                    .statusCode(204)

                // Confirm updated amount
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .cookie("SESSION", session)
                    .get("$BOOKING_API_PATH/bookings/${bookingId}")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.id", equalTo(bookingId.toInt()))
                    .body("data.amount", equalTo(newAmount))

                true
            }
    }

    @Test
    fun `AMQP - delete trip leads to cancelled bookings`() {
        Awaitility.await().atMost(145, TimeUnit.SECONDS)
            .pollInterval(fibonacci(TimeUnit.SECONDS))
            .ignoreExceptions()
            .until {
                val capacity = 5
                // Register a Trip
                val tripId = registerTrip(capacity = capacity)
                // Create some bookings on the trip
                val session = getRegisteredSessionCookie()
                val bookingIds = mutableListOf<Long>()
                for (x in 0 until capacity) {
                    bookingIds.add(registerBooking(session, tripId, 1))
                }
                // Delete the Trip - this should cancel all the Bookings
                val adminSession = getAdminSession()
                RestAssured.given()
                    .cookie("SESSION", adminSession)
                    .contentType(ContentType.JSON)
                    .delete("$TRIP_API_PATH/trips/$tripId")
                    .then().assertThat()
                    .statusCode(204)

                // Verify that the bookings are now marked as cancelled
                bookingIds.forEach {
                    RestAssured.given()
                        .accept(ContentType.JSON)
                        .cookie("SESSION", session)
                        .get("$BOOKING_API_PATH/bookings/${it}")
                        .then().assertThat()
                        .statusCode(200)
                        .body("data.id", equalTo(it.toInt()))
                        .body("data.cancelled", equalTo(true))
                }
                true
            }
    }

    private fun Response.getNextLink(): String? {
        return this.jsonPath().get("data.next")
    }

}