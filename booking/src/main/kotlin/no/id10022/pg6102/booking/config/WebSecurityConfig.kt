package no.id10022.pg6102.booking.config

import no.id10022.pg6102.booking.BOOKINGS_API_PATH
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http
            // Exception Handling
            .exceptionHandling().authenticationEntryPoint { _, res, _ ->
                res.setHeader("WWW-Authenticate", "cookie")
                res.sendError(401)
            }
            .and()
            // Authorization
            .authorizeRequests()
            // Actuator endpoints
            .antMatchers("/actuator/**").permitAll()
            // API Documentation endpoint
            .antMatchers("/swagger*/**", "/v3/api-docs").permitAll()
            // Service endpoints
            .antMatchers(HttpMethod.GET, "$BOOKINGS_API_PATH*/**").permitAll()
            .antMatchers(HttpMethod.HEAD, "$BOOKINGS_API_PATH*/**").permitAll()
            .antMatchers("$BOOKINGS_API_PATH*/**").hasRole("ADMIN")
            // Block anything else
            .anyRequest().denyAll()
            .and()
            // Other
            .csrf().disable()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER)
    }
}