package no.id10022.pg6102.auth

import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
    SpringApplication.run(AuthApplication::class.java, "--spring.profiles.active=dev")
}