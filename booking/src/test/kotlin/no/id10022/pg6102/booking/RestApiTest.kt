package no.id10022.pg6102.booking

import io.restassured.RestAssured
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [(BookingApplication::class)],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class RestApiTest(){

    @LocalServerPort
    protected var port = 0

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
    }
}