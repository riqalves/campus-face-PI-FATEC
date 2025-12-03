package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.ApiResponse
import br.com.fatec.campusface.dto.LoginDTO
import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.repository.UserRepository
import br.com.fatec.campusface.service.AuthService
import br.com.fatec.campusface.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.Base64

@RestController
@RequestMapping("/auth")
class AuthController() {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @PostMapping("/login")
    fun login(@RequestBody @Valid data: LoginDTO): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            if (!userService.validateEmail(data.email)) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse(message = "Padrão de email invalido (@)", success = false, data = null))
            }

            // 1. Cria o token com as credenciais recebidas
            val usernamePassword = UsernamePasswordAuthenticationToken(data.email, data.password)

            // 2. CORREÇÃO: Chama o AuthenticationManager para verificar a senha!
            // Se a senha estiver errada, ele lança AuthenticationException e cai no catch.
            val auth = authenticationManager.authenticate(usernamePassword)

            // 3. Se passou (não deu erro), busca os dados do usuário
            val userDto = userService.getUserByEmail(data.email)
                ?: throw UsernameNotFoundException("Usuário não encontrado")

            val token = authService.generateToken(userDto)

            val responseBody = mapOf(
                "token" to token,
                "user" to userDto
            )

            ResponseEntity.ok(
                ApiResponse(
                    message = "Login realizado com sucesso",
                    success = true,
                    data = responseBody
                )
            )
        } catch (e: AuthenticationException) {
            // Agora sim este bloco será alcançado quando a senha for inválida
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse(message = "Credenciais inválidas: ${e.message}", success = false, data = null))
        }
    }


    @PostMapping("/register", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun register(
        @RequestParam("fullName") fullName: String,
        @RequestParam("email") email: String,
        @RequestParam("hashedPassword") password: String,
        @RequestParam("document") document: String,
        @RequestPart("image", required = false) image: MultipartFile?
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val userData = User(
                fullName = fullName,
                email = email,
                hashedPassword = password,
                document = document
            )
            println("DEBUG: $userData")

            val createdUserDto = userService.createUser(userData, image)

            ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "Usuário registrado e associado à organização com sucesso!",
                    data = createdUserDto
                )
            )

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(
                    success = false,
                    message = "Ocorreu um erro. Argumento Ilegal ${e.message}",
                    data = null
                )
            )
        } catch (e: Exception) {
            println("Erro ao registrar usuário: ${e.message}")
            e.printStackTrace()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    message = "Ocorreu um erro inesperado ao registrar o usuário.",
                    success = false,
                    data = null
                )
            )
        }
    }

}
