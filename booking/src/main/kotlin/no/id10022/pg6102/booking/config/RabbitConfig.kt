package no.id10022.pg6102.booking.config

import no.id10022.pg6102.utils.amqp.*
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    @Bean
    fun authDx(): DirectExchange {
        return DirectExchange(authExchangeName)
    }

    @Bean
    fun createUserQueue(): Queue {
        return Queue(createUserBookingQueue)
    }

    @Bean
    fun createUserBinding(
        authDx: DirectExchange,
        createUserQueue: Queue
    ): Binding {
        return BindingBuilder
            .bind(createUserQueue)
            .to(authDx)
            .with(createUserRK)
    }

    @Bean
    fun tripDx(): DirectExchange {
        return DirectExchange(tripExchangeName)
    }

    @Bean
    fun createTripQueue(): Queue {
        return Queue(createTripBookingQueue)
    }

    @Bean
    fun createTripBinding(
        tripDx: DirectExchange,
        createTripQueue: Queue
    ): Binding {
        return BindingBuilder
            .bind(createTripQueue)
            .to(tripDx)
            .with(createTripRK)
    }

    @Bean
    fun deleteTripQueue(): Queue {
        return Queue(deleteTripBookingQueue)
    }

    @Bean
    fun deleteTripBinding(
        tripDx: DirectExchange,
        deleteTripQueue: Queue
    ): Binding {
        return BindingBuilder
            .bind(deleteTripQueue)
            .to(tripDx)
            .with(deleteTripRK)
    }

}