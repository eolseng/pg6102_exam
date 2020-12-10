package no.id10022.pg6102.trip

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import no.id10022.pg6102.trip.db.TripRepository
import no.id10022.pg6102.utils.rest.dto.TripDto
import org.hamcrest.CoreMatchers
import org.junit.Assert.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import java.time.LocalDateTime
import kotlin.random.Random

@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [(TripApplication::class)],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [(RestApiTest.Companion.Initializer::class)])
class RestApiTest {

    @LocalServerPort
    protected var port = 0

    @Autowired
    lateinit var repo: TripRepository

    @BeforeEach
    @AfterEach
    fun setup() {
        // Setup RestAssured
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        RestAssured.basePath = TRIPS_API_PATH
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        // Clear repository
        repo.deleteAll()
    }

    companion object {
        // Generates unique IDs
        private var idCounter = 1
        fun getId(): Int {
            return idCounter++
        }

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

        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
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

    fun registerTrip(
        startDelay: Long = 0,
        durationInMinutes: Long = Random.nextLong(30, 14440),
        price: Int = Random.nextInt(25, 500),
        capacity: Int = Random.nextInt(1, 20)
    ): Long {
        // Create the DTO
        val id = getId()
        val start = LocalDateTime.now().plusDays(1 + startDelay)
        val dto = TripDto(
            title = "test_title_$id",
            description = "test_description_$id",
            location = "test_location_$id",
            start = start,
            end = start.plusMinutes(durationInMinutes),
            price = price,
            capacity = capacity
        )
        // Register the Trip and extract the Location Header
        val redirect =
            RestAssured.given()
                .auth().basic("admin", "admin")
                .contentType(ContentType.JSON)
                .body(dto)
                .post()
                .then().assertThat()
                .statusCode(201)
                .extract()
                .header("Location")
        // Extract the ID from the Location Header and return it
        return redirect.substringAfter("${RestAssured.basePath}/").toLong()
    }

    @Test
    fun `register Trip`() {
        val id = registerTrip()
        val trip = repo.findById(id)
        assertTrue(trip.isPresent)
    }

    @Test
    fun `retrieve Trip by id`() {
        val id = registerTrip()
        val trip = repo.findById(id).get()

        RestAssured.given()
            .accept(ContentType.JSON)
            .get("/$id")
            .then().assertThat()
            .statusCode(200)
            .body("data.id", CoreMatchers.equalTo(id.toInt()))
            .body("data.title", CoreMatchers.equalTo(trip.title))
            .body("data.description", CoreMatchers.equalTo(trip.description))
            .body("data.location", CoreMatchers.equalTo(trip.location))
            .body("data.duration.days", CoreMatchers.equalTo(trip.duration["days"]))
            .body("data.duration.hours", CoreMatchers.equalTo(trip.duration["hours"]))
            .body("data.duration.minutes", CoreMatchers.equalTo(trip.duration["minutes"]))
            .body("data.price", CoreMatchers.equalTo(trip.price))
            .body("data.capacity", CoreMatchers.equalTo(trip.capacity))
    }

    @Test
    fun `get all Trips - single page`() {
        val amount = 10
        // Register n Trips
        for (x in 0 until amount) {
            registerTrip()
        }
        // Verify with repository
        assertEquals(amount, repo.count().toInt())

        RestAssured.given()
            .accept(ContentType.JSON)
            .get("?amount=${amount + 1}")
            .then().assertThat()
            .statusCode(200)
            .body("data.list.size()", CoreMatchers.equalTo(amount))
            .body("data.next", CoreMatchers.nullValue())
    }

    @Test
    fun `get all Trips - multiple pages`() {
        val pages = 4
        val pageSize = 5
        val total = pages * pageSize
        val uniqueIds = mutableSetOf<Long>()

        // Register n Trips
        for (x in 0 until total) {
            registerTrip()
        }
        // Verify with repository
        assertEquals(total, repo.count().toInt())
        // Get first page
        var res = RestAssured.given()
            .accept(ContentType.JSON)
            .get("?amount=$pageSize")
            .then().assertThat()
            .statusCode(200)
            .body("data.list.size()", CoreMatchers.equalTo(pageSize))
            .body("data.next", CoreMatchers.anything())
            .extract()
            .response()
        var dtos = res.getTrips()
        var next = res.getNextLink()
        // Add IDs to unique ID list
        uniqueIds.addAll(dtos.map { it.id!! })
        // Check the ordering
        dtos.checkOrder()
        // Check rest of the pages
        while (next != null) {
            res = RestAssured.given()
                .accept(ContentType.JSON)
                .basePath("")
                .get(next)
                .then().assertThat()
                .statusCode(200)
                .extract()
                .response()
            dtos = res.getTrips()
            next = res.getNextLink()
            // Add IDs to unique ID list
            uniqueIds.addAll(dtos.map { it.id!! })
            // Check the ordering
            dtos.checkOrder()
        }
        // Check that all Trips have been retrieved
        assertEquals(total, uniqueIds.size)
    }

    @Test
    fun `patch trip with valid updates`() {

        // Register and get Trip
        val id = registerTrip()
        val originalTrip = repo.findById(id).get()

        // Declare data for patch
        val newTitle = "new_title"
        val newDescription = "new_description"
        val newLocation = "new_location"
        val newStart = LocalDateTime.now().plusYears(1)
        val newEnd = newStart.plusDays(1)
        val newPrice = 800
        val newCapacity = 40

        // Create patch JsonDto
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
            .patch("/$id")
            .then().assertThat()
            .statusCode(204)

        // Get the patched Trip
        val patchedTrip = repo.findById(id).get()

        // Verify update
        assertTrue(patchedTrip.title != originalTrip.title)
        assertTrue(patchedTrip.title == newTitle)

        assertTrue(patchedTrip.description != originalTrip.description)
        assertTrue(patchedTrip.description == newDescription)

        assertTrue(patchedTrip.location != originalTrip.location)
        assertTrue(patchedTrip.location == newLocation)

        assertTrue(patchedTrip.start != originalTrip.start)
        assertTrue(patchedTrip.start == newStart)

        assertTrue(patchedTrip.end != originalTrip.end)
        assertTrue(patchedTrip.end == newEnd)

        assertTrue(patchedTrip.price == newPrice)

        assertTrue(patchedTrip.capacity != originalTrip.capacity)
        assertTrue(patchedTrip.capacity == newCapacity)

    }

    @Test
    fun `patch with invalid updates`() {
        // Register and get Trip
        val id = registerTrip()
        val om = ObjectMapper()

        // Null string
        val nullString = om.createObjectNode()
            .putNull("title")
        RestAssured.given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(nullString)
            .patch("/$id")
            .then().assertThat()
            .statusCode(409)

        // Empty string
        val emptyString = om.createObjectNode()
            .put("description", "")
        RestAssured.given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(emptyString)
            .patch("/$id")
            .then().assertThat()
            .statusCode(409)

        // String as date
        val invalidDate = om.createObjectNode()
            .put("start", "INVALID")
        RestAssured.given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(invalidDate)
            .patch("/$id")
            .then().assertThat()
            .statusCode(400)

        // String as int
        val stringAsInt = om.createObjectNode()
            .put("price", "STRING")
        RestAssured.given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(stringAsInt)
            .patch("/$id")
            .then().assertThat()
            .statusCode(400)

    }

    @Test
    fun `patch security`() {

        val id = registerTrip()
        val patchDto = ObjectMapper().createObjectNode()
            .put("title", "new title")

        // Not authenticated
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(patchDto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(401)

        // As Aser
        RestAssured.given()
            .auth().basic("user", "user")
            .contentType(ContentType.JSON)
            .body(patchDto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(403)

        // As Admin
        RestAssured.given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(patchDto)
            .patch("/$id")
            .then().assertThat()
            .statusCode(204)

        // Invalid ID
        val invalidId = id + 1337
        assertFalse(repo.existsById(invalidId))
        RestAssured.given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(patchDto)
            .patch("/$invalidId")
            .then().assertThat()
            .statusCode(404)

    }

    @Test
    fun `delete Trip by id`() {

        val id = registerTrip()
        assertTrue(repo.existsById(id))

        // Not authenticated
        RestAssured.given()
            .accept(ContentType.JSON)
            .delete("/$id")
            .then().assertThat()
            .statusCode(401)

        // As User
        RestAssured.given()
            .auth().basic("user", "user")
            .accept(ContentType.JSON)
            .delete("/$id")
            .then().assertThat()
            .statusCode(403)

        // As Admin
        RestAssured.given()
            .auth().basic("admin", "admin")
            .accept(ContentType.JSON)
            .delete("/$id")
            .then().assertThat()
            .statusCode(204)

        // Invalid ID
        val invalidId = id + 1337
        assertFalse(repo.existsById(invalidId))
        RestAssured.given()
            .auth().basic("admin", "admin")
            .accept(ContentType.JSON)
            .delete("/$invalidId")
            .then().assertThat()
            .statusCode(404)

    }

    fun Response.getTrips(): List<TripDto> {
        return this.jsonPath().getList("data.list", TripDto::class.java)
    }

    fun Response.getNextLink(): String? {
        return this.jsonPath().get("data.next")
    }

    fun List<TripDto>.checkOrder() {
        for (i in 0 until this.size - 1) {
            assertTrue(this[i].start!! <= this[i + 1].start!!)
            if (this[i].start!! == this[i + 1].start!!) {
                assertTrue(this[i].id!! <= this[i + 1].id!!)
            }
        }
    }

}