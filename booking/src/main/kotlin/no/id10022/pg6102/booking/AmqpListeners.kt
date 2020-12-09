package no.id10022.pg6102.booking

import no.id10022.pg6102.booking.service.TripService
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
    private val userService: UserService,
    private val tripService: TripService
) {

    val logger: Logger = LoggerFactory.getLogger(AmqpListeners::class.java)

    fun logReceived(msg: String) {
        logger.info("AMQP Received: $msg")
    }

    @RabbitListener(queues = [createUserBookingQueue])
    fun createUserOnMessage(username: String) {
        logReceived("Create User[username=$username]")
        userService.createUser(username)
    }

    @RabbitListener(queues = [createTripBookingQueue])
    fun createTripOnMessage(id: Long) {
        logReceived("Create Trip[id=$id]")
        tripService.createTrip(id)
    }

    /**
     * When a Trip is deleted we mark it and all Bookings as Cancelled
     * This way a Booking still has a reference to the cancelled Trips ID, even though the Trip itself is gone
     */
    @RabbitListener(queues = [deleteTripBookingQueue])
    fun deleteTripOnMessage(id: Long) {
        logReceived("Delete Trip[id=$id]")
        tripService.cancelTrip(id)
    }

}