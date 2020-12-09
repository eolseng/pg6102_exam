package no.id10022.pg6102.booking.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import javax.persistence.LockModeType

interface UserRepository : JpaRepository<User, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.username = :username")
    fun findWithLock(@Param("username") username: String): User?
}