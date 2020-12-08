package no.id10022.pg6102.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import javax.sql.DataSource

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val dataSource: DataSource
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
            // Exception Handling
            .exceptionHandling().authenticationEntryPoint { _, res, _ ->
                res.setHeader("WWW-Authenticate", "cookie")
                res.sendError(401)
            }
            .and()

            // Logout
            .logout().logoutUrl("/api/v1/auth/logout")
            .logoutSuccessHandler((HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .and()

            // Authorization
            .authorizeRequests()
            // Actuator endpoints
            .antMatchers("/actuator/**").permitAll()
            // Swagger endpoints
            .antMatchers("/swagger*/**", "/v3/api-docs").permitAll()
            // Service endpoints
            .antMatchers("/api/v1/auth/signup").permitAll()
            .antMatchers("/api/v1/auth/login").permitAll()
            .antMatchers("/api/v1/auth/logout").permitAll()
            .antMatchers("/api/v1/auth/user").authenticated()
            // Block anything else
            .anyRequest().denyAll()
            .and()

            // Other
            .csrf().disable()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    override fun userDetailsServiceBean(): UserDetailsService {
        return super.userDetailsServiceBean()
    }


    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.jdbcAuthentication()
            .dataSource(dataSource)
            .usersByUsernameQuery(
                """
                     SELECT username, password, enabled
                     FROM users
                     WHERE username=?
                     """
            )
            .authoritiesByUsernameQuery(
                """
                     SELECT x.username, y.authority
                     FROM users x, authorities y
                     WHERE x.username=? and y.username=x.username
                     """
            )
            .passwordEncoder(passwordEncoder())
    }
}