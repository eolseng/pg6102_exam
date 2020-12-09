package no.id10022.pg6102.booking.db

import org.springframework.data.jpa.repository.JpaRepository

interface TripRepository : JpaRepository<Trip, Long>