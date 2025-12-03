package br.com.fatec.campusface.configuration

import br.com.fatec.campusface.repository.UserRepository
import br.com.fatec.campusface.service.AuthService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SecurityFilter : OncePerRequestFilter() {


    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    lateinit var authService: AuthService

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val path = request.requestURI

        // Ignora rotas públicas
        if (path.startsWith("/auth/register") || path.startsWith("/auth/login")) {
            filterChain.doFilter(request, response)
            return
        }


        val token = this.recoverToken(request)
        if (token != null) {
            val id = authService.validateToken(token)

            if (id.isNotBlank()) {
//                val user = userRepository.findUserDetailById(id)
                val user = userRepository.findById(id)
                println("SECURITY FILTER: $user")
                val authentication = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user?.authorities
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun recoverToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization") ?: return null
        return authHeader.replace("Bearer ", "")

    }
}