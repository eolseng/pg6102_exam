package no.id10022.pg6102.auth

import no.id10022.pg6102.auth.db.UserRepository
import no.id10022.pg6102.auth.db.UserService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component


@Component
class InitialDataLoader(
    private val repo: UserRepository,
    private val service: UserService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        createAdminUser()
    }

    /**
     * Creates a Admin user for the service
     * --------- [HIGHLY INSECURE] ---------
     *  Only implemented for demo purposes!
     * MUST BE REMOVED IF USED IN PRODUCTION
     * -------------------------------------
     */
    private fun createAdminUser() {
        if (!repo.existsById("admin")) {
            service.createUser("admin", "admin", setOf("USER", "ADMIN"))
        }
    }

}