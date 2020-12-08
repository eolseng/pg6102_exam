package no.id10022.pg6102.auth.db

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, String>