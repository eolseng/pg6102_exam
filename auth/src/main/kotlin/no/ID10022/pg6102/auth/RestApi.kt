package no.id10022.pg6102.auth

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.id10022.pg6102.auth.db.UserService
import no.id10022.pg6102.auth.dto.AuthDto
import no.id10022.pg6102.utils.amqp.authExchangeName
import no.id10022.pg6102.utils.amqp.createUserRK
import no.id10022.pg6102.utils.rest.RestResponseFactory
import no.id10022.pg6102.utils.rest.WrappedResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import java.net.URI

@Api(value = "/api/v1/auth", description = "Authorization API for signup, login, logout and roles")
@RestController
@RequestMapping("/api/v1/auth")
class RestApi(
    private val service: UserService,
    private val authManager: AuthenticationManager,
    private val userDetailsServiceBean: UserDetailsService,
    private val rabbitMQ: RabbitTemplate
) {

    val logger : Logger = LoggerFactory.getLogger(RestApi::class.java)

    @ApiOperation("Retrieve name and roles of signed in user")
    @GetMapping("/user")
    fun user(user: Authentication): ResponseEntity<WrappedResponse<Map<String, Any>>> {
        val map = mutableMapOf<String, Any>()
        map["username"] = user.name
        map["roles"] = AuthorityUtils.authorityListToSet(user.authorities)
        return RestResponseFactory.payload(200, map)
    }

    @ApiOperation("Create a new user")
    @PostMapping(
        path = ["/signup"],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun signup(@RequestBody dto: AuthDto): ResponseEntity<WrappedResponse<Void>> {

        // Extract data from the DTO - lower casing for easier logons
        val username = dto.username.toLowerCase()
        val password = dto.password

        // Attempt to register user
        val registered = service.createUser(username, password, setOf("USER"))
        if (!registered) return RestResponseFactory.userError(message = "Username already exists")

        // Publish message that a new user is created
        rabbitMQ.convertAndSend(authExchangeName, createUserRK, username)

        // Attempt to retrieve the user from database
        val userDetails = try {
            userDetailsServiceBean.loadUserByUsername(username)
        } catch (e: UsernameNotFoundException) {
            return RestResponseFactory.serverFailure("Could not retrieve user from database")
        }

        // Create authentication token and authenticate it
        val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)
        authManager.authenticate(token)
        if (token.isAuthenticated) SecurityContextHolder.getContext().authentication = token

        // User successfully created - log and redirect to /user endpoint
        logger.info("Created User[username=$username]")
        return RestResponseFactory.created(URI.create("/api/v1/auth/user"))

    }

    @ApiOperation("Login a registered user")
    @PostMapping(
        path = ["/login"],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun login(@RequestBody dto: AuthDto): ResponseEntity<WrappedResponse<Void>> {

        // Extract data from the DTO
        val username = dto.username.toLowerCase()
        val password = dto.password

        // Attempt to retrieve the user from database
        val userDetails = try {
            userDetailsServiceBean.loadUserByUsername(username)
        } catch (e: UsernameNotFoundException) {
            return RestResponseFactory.userError("Username not found")
        }

        // Create authentication token and authenticate it
        val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)
        authManager.authenticate(token)
        if (token.isAuthenticated) {
            SecurityContextHolder.getContext().authentication = token
            logger.info("Logged in User[username=$username]")
            return RestResponseFactory.noPayload(204)
        }

        // Fallback in case authentication fails - Wrong password gets handled by 'authManager.authenticate(token)'
        return RestResponseFactory.userError("Authentication failed")
    }

}