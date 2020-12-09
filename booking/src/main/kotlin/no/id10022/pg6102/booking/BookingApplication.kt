package no.id10022.pg6102.booking

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

const val API_BASE_PATH = "/api/v1/booking"

@EnableDiscoveryClient
@SpringBootApplication
class BookingApplication

fun main(args: Array<String>) {
    SpringApplication.run(BookingApplication::class.java, *args)
}