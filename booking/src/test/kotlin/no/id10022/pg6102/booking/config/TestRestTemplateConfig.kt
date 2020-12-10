package no.id10022.pg6102.booking.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class TestRestTemplateConfig {

    // Remove @LoadBalanced since we do not use Consul in tests
    @Bean
    fun client(): RestTemplate {
        return RestTemplate()
    }

}