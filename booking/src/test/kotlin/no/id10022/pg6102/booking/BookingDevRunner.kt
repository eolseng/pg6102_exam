package no.id10022.pg6102.booking

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
    SpringApplication.run(BookingApplication::class.java, "--spring.profiles.active=dev")
}