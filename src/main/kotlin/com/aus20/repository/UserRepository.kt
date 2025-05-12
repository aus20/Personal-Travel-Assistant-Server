package com.aus20.repository

import com.aus20.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository //Tells Spring to treat it as a database access component
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}
