package br.com.fatec.campusface.controller

import br.com.fatec.campusface.configuration.SecurityFilter
import br.com.fatec.campusface.dto.LoginDTO
import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.repository.UserRepository
import br.com.fatec.campusface.service.AuthService
import br.com.fatec.campusface.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var userService: UserService

    @MockkBean
    lateinit var authService: AuthService

    @MockkBean
    lateinit var authenticationManager: AuthenticationManager

    @MockkBean
    lateinit var securityFilter: SecurityFilter
    @MockkBean
    lateinit var userRepository: UserRepository

    // =========================================================================
    // TESTES DE LOGIN
    // =========================================================================

    @Test
    fun `login deve retornar 200 e token quando credenciais sao validas`() {
        val loginData = LoginDTO("teste@fatec.sp.gov.br", "123456")
        val userDto = UserDTO(id="u1", fullName="User", email=loginData.email, document="123", faceImageId="f", createdAt=Instant.now(), updatedAt=Instant.now())

        every { userService.validateEmail(loginData.email) } returns true

        // Mock genérico para qualquer objeto Authentication
        every { authenticationManager.authenticate(any()) } returns UsernamePasswordAuthenticationToken(loginData.email, loginData.password)

        every { userService.getUserByEmail(loginData.email) } returns userDto
        every { authService.generateToken(userDto) } returns "token-jwt-valido"

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginData)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").value("token-jwt-valido"))
    }

    @Test
    fun `login deve retornar 401 Unauthorized se email for validado como incorreto pelo service`() {
        // O teste é para garantir que a regra de negócio (userService.validateEmail) retorna 401
        val loginData = LoginDTO("email@formato.ok", "123")

        // Simulamos que, apesar do formato ok, o service rejeitou (regra de negócio)
        every { userService.validateEmail(loginData.email) } returns false

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginData)))
            .andExpect(status().isUnauthorized) // Espera 401
            .andExpect(jsonPath("$.message").value("Padrão de email invalido (@)"))
    }

    @Test
    fun `login deve retornar 401 Unauthorized se senha estiver incorreta`() {
        val loginData = LoginDTO("teste@fatec.sp.gov.br", "senhaErrada")

        every { userService.validateEmail(loginData.email) } returns true

        // CORREÇÃO:
        // 1. Usamos 'throws' direto (mais simples que answers)
        // 2. Especificamos 'any(Authentication::class)' para ajudar o MockK a encontrar o método certo
        every { authenticationManager.authenticate(any(Authentication::class)) } throws BadCredentialsException("Senha incorreta")

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginData)))
            .andExpect(status().isUnauthorized) // HTTP 401
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Credenciais inválidas: Senha incorreta"))
    }

    // =========================================================================
    // TESTES DE REGISTRO
    // =========================================================================

    @Test
    fun `register deve retornar 201 Created ao registrar usuario com sucesso`() {
        val imageFile = MockMultipartFile("image", "face.jpg", "image/jpeg", "bytes".toByteArray())
        val userDto = UserDTO(id="u1", fullName="Novo User", email="new@test.com", document="doc", faceImageId="img", createdAt=Instant.now(), updatedAt=Instant.now())

        every { userService.createUser(any(), any()) } returns userDto

        mockMvc.perform(multipart("/auth/register")
            .file(imageFile)
            .param("fullName", "Novo User")
            .param("email", "new@test.com")
            .param("hashedPassword", "123456")
            .param("document", "12345678900"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fullName").value("Novo User"))
    }

    @Test
    fun `register deve retornar 400 Bad Request se houver erro de validacao no service`() {
        every { userService.createUser(any(), any()) } throws IllegalArgumentException("Email já existe")

        mockMvc.perform(multipart("/auth/register")
            .param("fullName", "User")
            .param("email", "existente@test.com")
            .param("hashedPassword", "123")
            .param("document", "123"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Ocorreu um erro. Argumento Ilegal Email já existe"))
    }
}