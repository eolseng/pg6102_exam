package no.id10022.pg6102.trip

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
    SpringApplication.run(TripApplication::class.java, "--spring.profiles.active=test")
}