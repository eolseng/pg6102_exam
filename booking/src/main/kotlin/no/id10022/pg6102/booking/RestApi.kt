package no.id10022.pg6102.booking

import io.swagger.annotations.Api
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

const val BOOKINGS_API_PATH = "$API_BASE_PATH/bookings"

@Api(value = BOOKINGS_API_PATH, description = "Endpoint for managing Bookings")
@RestController
@RequestMapping(
    path = [BOOKINGS_API_PATH],
    produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
class RestApi {



}