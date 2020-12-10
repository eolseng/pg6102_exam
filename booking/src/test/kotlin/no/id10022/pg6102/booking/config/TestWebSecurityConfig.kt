package no.id10022.pg6102.booking.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint

@Order(1)
@Configuration
@EnableWebSecurity
class TestWebSecurityConfig : WebSecurityConfig() {

    override fun configure(http: HttpSecurity) {
        super.configure(http)
        http
            .httpBasic()
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(BasicAuthenticationEntryPoint())
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.inMemoryAuthentication()
            .withUser("admin").password("{noop}admin").roles("ADMIN", "USER")
            .and()
            .withUser("user").password("{noop}user").roles("USER")
            .and()
            .withUser("extra").password("{noop}extra").roles("USER")
    }

}