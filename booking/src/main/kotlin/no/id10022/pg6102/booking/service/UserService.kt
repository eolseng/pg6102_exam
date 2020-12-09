package no.id10022.pg6102.booking.service

import no.id10022.pg6102.booking.db.User
import no.id10022.pg6102.booking.db.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val repo: UserRepository
) {

    val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * Creates a local copy of a User.
     * Usually gets called from an AMQP triggered event.
     */
    fun createUser(username: String): User {
        val user = repo.save(User(username = username))
        logger.info("Created User[username=$username]")
        return user
    }

    /**
     * Attempts to retrieve a User from the local repository.
     * If the User does not exits we create it.
     * Only safe to call with username verified by Authentication!
     *
     * Todo: Implement verification with Auth Service. Auth must implement username-based check.
     */
    fun getUserByUsername(username: String, locked: Boolean): User? {
        if (!repo.existsById(username)) createUser(username)
        return if (locked) repo.findWithLock(username) else repo.findByIdOrNull(username)
    }

}