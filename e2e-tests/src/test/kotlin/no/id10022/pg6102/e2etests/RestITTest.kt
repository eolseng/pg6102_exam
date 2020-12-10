package no.id10022.pg6102.e2etests

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import no.id10022.pg6102.auth.dto.AuthDto
import no.id10022.pg6102.utils.rest.dto.TripDto
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers.*
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
            .withLogConsumer("gateway") { print("[GATEWAY] ${it.utf8String}") }
            .withLogConsumer("auth") { print("[AUTH   ] ${it.utf8String}") }
            .withLogConsumer("trip_0") { print("[TRIP_0 ] ${it.utf8String}") }
            .withLogConsumer("trip_1") { print("[TRIP_1 ] ${it.utf8String}") }
            .withLogConsumer("booking") { print("[BOOKING] ${it.utf8String}") }

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
     * @return the authenticated 'SESSION' cookie
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
     * Returns a valid Admin Session Cookie
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
     * Utility function to register a Trip
     * Requires a valid Admin Session
     * @return the ID of the created Trip
     */
    private fun registerTrip(
        adminSession: String,
        startDelay: Long = 0,
        durationInMinutes: Long = Random.nextLong(30, 14440),
        price: Int = Random.nextInt(25, 500),
        capacity: Int = Random.nextInt(1, 20)
    ): Long {
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
                .cookie("SESSION", adminSession)
                .contentType(ContentType.JSON)
                .body(dto)
                .post("$TRIP_API_PATH/trips")
                .then().assertThat()
                .statusCode(201)
                .extract()
                .header("Location")
        // Extract the ID from the Location Header and return it
        return redirect.substringAfter("${RestAssured.basePath}/").toLong()
    }

    @Test
    fun `auth - user-endpoint - unauthorized`() {
        Awaitility.await().atMost(120, TimeUnit.SECONDS)
            .pollInterval(Duration.ofSeconds(10))
            .ignoreExceptions()
            .until {
                RestAssured.given()
                    .get("$AUTH_API_PATH/user")
                    .then().assertThat()
                    .statusCode(401)
                true
            }
    }

    @Test
    fun `auth - user-endpoint - authenticated`() {
        Awaitility.await().atMost(120, TimeUnit.SECONDS)
            .pollInterval(Duration.ofSeconds(10))
            .ignoreExceptions()
            .until {
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
    fun `auth - logout`() {

        Awaitility.await().atMost(120, TimeUnit.SECONDS)
            .pollInterval(Duration.ofSeconds(10))
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
        Awaitility.await().atMost(120, TimeUnit.SECONDS)
            .pollInterval(Duration.ofSeconds(10))
            .ignoreExceptions()
            .until {
                val adminSession = getAdminSession()
                registerTrip(adminSession)
                true
            }
    }

    @Test
    fun `trip - trips-endpoint - get single trip`() {
        Awaitility.await().atMost(120, TimeUnit.SECONDS)
            .pollInterval(Duration.ofSeconds(10))
            .ignoreExceptions()
            .until {
                val adminSession = getAdminSession()
                val price = 789
                val capacity = 88
                val id = registerTrip(adminSession = adminSession, price = price, capacity = capacity)
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
    fun `trip - trips-endpoint - get multiple trips`() {
        Awaitility.await().atMost(120, TimeUnit.SECONDS)
            .pollInterval(Duration.ofSeconds(10))
            .ignoreExceptions()
            .until {
                val adminSession = getAdminSession()
                // Register multiple trips - low number to reduce test times
                val half = 2
                val amount = half * 2
                for (x in 0 until amount) {
                    registerTrip(adminSession)
                }
                // Get all in a single page
                // Asking for huge page size in case of other tests registering Trips
                RestAssured.given()
                    .accept(ContentType.JSON)
                    .get("$TRIP_API_PATH/trips?amount=${amount * 20}")
                    .then().assertThat()
                    .statusCode(200)
                    .body("data.list.size()", equalTo(amount))
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
    fun `trip - trips-endpoint - patch`() {
        Awaitility.await().atMost(120, TimeUnit.SECONDS)
            .pollInterval(Duration.ofSeconds(10))
            .ignoreExceptions()
            .until {
                val adminSession = getAdminSession()
                val id = registerTrip(adminSession = adminSession)

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
                    .auth().basic("admin", "admin")
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

    private fun Response.getNextLink(): String? {
        return this.jsonPath().get("data.next")
    }

}