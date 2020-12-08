package no.id10022.pg6102.trip.config

import no.id10022.pg6102.utils.amqp.tripExchangeName
import org.springframework.amqp.core.DirectExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    @Bean
    fun tripDx(): DirectExchange {
        return DirectExchange(tripExchangeName)
    }

}