package no.id10022.pg6102.gateway

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
    SpringApplication.run(GatewayApplication::class.java, "--spring.profiles.active=dev")
}