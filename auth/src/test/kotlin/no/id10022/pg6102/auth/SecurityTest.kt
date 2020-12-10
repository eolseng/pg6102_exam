package no.id10022.pg6102.auth

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import no.id10022.pg6102.auth.db.UserRepository
import no.id10022.pg6102.auth.dto.AuthDto
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
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

@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [(SecurityTest.Companion.Initializer::class)])
class SecurityTest {

    @LocalServerPort
    private var port = 0

    @Autowired
    private lateinit var repository: UserRepository

    companion object {

        // ID counter for creating users
        private var idCounter = 0
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

        // Setup environment to use the containers
        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(applicationContext: ConfigurableApplicationContext) {
                TestPropertyValues.of(
                    "spring.redis.host=${redis.containerIpAddress}",
                    "spring.redis.port=${redis.getMappedPort(6379)}",
                    "spring.rabbitmq.host=${rabbitMQ.containerIpAddress}",
                    "spring.rabbitmq.port=${rabbitMQ.getMappedPort(5672)}"
                ).applyTo(applicationContext.environment)
            }
        }

    }

    @BeforeEach
    fun initialize() {
        // Configure RestAssured
        RestAssured.baseURI = "http://localhost"
        RestAssured.basePath = "/api/v1/auth"
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()

        // Clean database before each test
        repository.deleteAll()
    }

    /**
     * Utility function to register a new user
     * @return the authenticated 'SESSION' cookie
     */
    private fun registerUser(authDto: AuthDto): String {
        // Get 201 on successful registration. Should have Location and Set-Cookie header.
        return given().contentType(ContentType.JSON)
            .body(authDto)
            .put("/signup/${authDto.username}")
            .then().assertThat()
            .statusCode(201)
            .header("Location", containsString("/api/v1/auth/user"))
            .header("Set-Cookie", containsString("SESSION"))
            .cookie("SESSION")
            .extract().cookie("SESSION")
    }

    /**
     * Utility function to check if the session cookie is valid
     */
    private fun checkAuthenticatedCookie(cookie: String, expectedCode: Int) {
        given().cookie("SESSION", cookie)
            .get("/user")
            .then().assertThat()
            .statusCode(expectedCode)
    }

    private fun getUniqueAuthDto(): AuthDto {
        return AuthDto(username = "test_username_${getId()}", password = "test_password")
    }

    @Test
    fun `test register user`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)
    }

    @Test
    fun `test already registered username`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        // Get 400 on failed registration. Body should contain message describing error.
        given().contentType(ContentType.JSON)
            .body(dto)
            .put("/signup/${dto.username}")
            .then().assertThat()
            .statusCode(400)
            .body("message", anything())
    }

    @Test
    fun `test valid login`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)

        // Get 204 on successful login.
        val login = given().contentType(ContentType.JSON)
            .body(dto)
            .post("/login")
            .then().assertThat()
            .statusCode(204)
            .cookie("SESSION")
            .extract().cookie("SESSION")

        // New login should create a new session cookie
        assertNotEquals(login, cookie)
        checkAuthenticatedCookie(login, 200)

    }

    @Test
    fun `test login with invalid username`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        val invalidPasswordUser = AuthDto(username = dto.username + "_fail", password = dto.password)

        // Get 400 on invalid username. Body should contain message with error description.
        val invalidLogin = given().contentType(ContentType.JSON)
            .body(invalidPasswordUser)
            .post("/login")
            .then().assertThat()
            .statusCode(400)
            .body("message", anything())
            .extract().cookie("SESSION")
        // Should not generate session cookie with bad username
        assertNull(invalidLogin)

    }

    @Test
    fun `test login with invalid password`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        val invalidPasswordUser = AuthDto(username = dto.username, password = dto.password + 123)

        // Get 401 on failed login. Body should contain message with error description.
        val invalidLogin = given().contentType(ContentType.JSON)
            .body(invalidPasswordUser)
            .post("/login")
            .then().assertThat()
            .statusCode(401)
            .body("message", anything())
            .cookie("SESSION")
            .extract().cookie("SESSION")
        // Should create new session cookie
        assertNotEquals(invalidLogin, cookie)
        // New cookie should not be valid
        checkAuthenticatedCookie(invalidLogin, 401)

    }

    @Test
    fun `test user endpoint - unauthorized`() {
        // Get 401 on unauthorized request
        given().get("/user")
            .then().assertThat()
            .statusCode(401)
    }

    @Test
    fun `test user endpoint - authorized`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        // Get 200 on authorized request. Body should have username and roles
        given().cookie("SESSION", cookie)
            .get("/user")
            .then().assertThat()
            .statusCode(200)
            .body("data.username", equalTo(dto.username))
            .body("data.roles", contains("ROLE_USER"))
    }

    @Test
    fun `test logout`() {
        val dto = getUniqueAuthDto()
        val cookie = registerUser(dto)
        checkAuthenticatedCookie(cookie, 200)

        // Get 204 on logout
        given().cookie("SESSION", cookie)
            .post("/logout")
            .then().assertThat()
            .statusCode(204)

        // Session cookie should be invalid after logout
        checkAuthenticatedCookie(cookie, 401)
    }

}