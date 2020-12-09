package no.id10022.pg6102.booking

import no.id10022.pg6102.booking.service.UserService
import no.id10022.pg6102.utils.amqp.createTripBookingQueue
import no.id10022.pg6102.utils.amqp.createUserBookingQueue
import no.id10022.pg6102.utils.amqp.deleteTripBookingQueue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Configuration

@Configuration
class AmqpListeners(
    private val userService: UserService
) {

    val logger: Logger = LoggerFactory.getLogger(AmqpListeners::class.java)

    @RabbitListener(queues = [createUserBookingQueue])
    fun createUserOnMessage(username: String) {
        logger.info("Msg: Create User[username=$username]")
        userService.createUser(username)
    }

    @RabbitListener(queues = [createTripBookingQueue])
    fun createTripOnMessage(id: Long) {
        logger.info("Msg: Creat Trip[id=$id]")
    }

    @RabbitListener(queues = [deleteTripBookingQueue])
    fun deleteTripOnMessage(id: Long) {
        logger.info("Msg: Delete Trip[id=$id]")
    }

}