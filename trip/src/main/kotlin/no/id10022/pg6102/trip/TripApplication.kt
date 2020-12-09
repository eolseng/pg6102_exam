package no.id10022.pg6102.trip

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

const val API_BASE_PATH = "/api/v1/trip"

@EnableDiscoveryClient
@SpringBootApplication
class TripApplication

fun main(args: Array<String>) {
    SpringApplication.run(TripApplication::class.java, *args)
}