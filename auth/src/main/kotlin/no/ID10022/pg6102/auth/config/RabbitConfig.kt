package no.id10022.pg6102.auth.config

import org.springframework.amqp.core.DirectExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val authExchangeName = "pg6102.auth.dx"
const val newUserRK = "new_user"

@Configuration
class RabbitConfig {

    @Bean
    fun authDx(): DirectExchange {
        return DirectExchange(authExchangeName)
    }

}