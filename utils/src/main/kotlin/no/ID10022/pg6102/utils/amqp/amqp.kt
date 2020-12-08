package no.id10022.pg6102.utils.amqp

/**
 * Declaration of Exchanges, Routing Keys and Queues for AMQP / RabbitMQ
 */

// Auth service
const val authExchangeName = "travel-agency.auth.dx"
const val createUserRK = "create_user"

// Trip service
const val tripExchangeName = "travel-agency.trip.dx"
const val createTripRK = "create_trip"
const val deleteTripRK = "delete_trip"