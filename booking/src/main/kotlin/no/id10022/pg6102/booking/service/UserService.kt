package no.id10022.pg6102.booking.service

import no.id10022.pg6102.booking.db.User
import no.id10022.pg6102.booking.db.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val repo: UserRepository
) {

    val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    fun createUser(username: String): User {
        logger.info("Created User[username=$username]")
        return repo.save(User(username = username))
    }

}