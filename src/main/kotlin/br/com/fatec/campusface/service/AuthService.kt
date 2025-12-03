package br.com.fatec.campusface.service

import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTVerificationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class AuthService(val userRepository: UserRepository) : UserDetailsService {

    @Value("\${jwt.secret}")
    private lateinit var secret : String

    fun generateToken(user : UserDTO) : String {
        try {
            val algorithm = Algorithm.HMAC256(secret)
            val token = JWT.create()
                .withIssuer("campusface")
                .withSubject(user.id)
                .withExpiresAt(genExpirationDate())
                .sign(algorithm)
            return token

        }catch (e: JWTCreationException){
            throw RuntimeException("Invalid JWT token", e)
        }
    }


    fun validateToken(token: String) : String{
        try {
            val algorithm = Algorithm.HMAC256(secret)
            return JWT.require(algorithm)
                .withIssuer("campusface")
                .build()
                .verify(token)
                .getSubject()
        }catch (e: JWTVerificationException){
            return e.message?:"Invalid JWT token"
        }
    }


    override fun loadUserByUsername(email: String): UserDetails {
        // Chame o método do repositório que retorna seu modelo customizado 'User'
        return userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("Usuário com e-mail: $email não encontrado")
    }


    private fun genExpirationDate() : Instant{
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"))
    }
}