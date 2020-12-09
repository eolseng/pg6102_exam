package no.id10022.pg6102.booking.config

import no.id10022.pg6102.utils.amqp.authExchangeName
import no.id10022.pg6102.utils.amqp.tripExchangeName
import org.springframework.amqp.core.DirectExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    @Bean
    fun authDx(): DirectExchange {
        return DirectExchange(authExchangeName)
    }

    @Bean
    fun tripDx(): DirectExchange {
        return DirectExchange(tripExchangeName)
    }

}