package br.com.fatec.campusface.controller

import br.com.fatec.campusface.configuration.SecurityFilter
import br.com.fatec.campusface.dto.GenerateCodeRequest
import br.com.fatec.campusface.dto.GeneratedCodeResponse
import br.com.fatec.campusface.dto.ValidateCodeRequest
import br.com.fatec.campusface.dto.ValidationResponseDTO
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.repository.UserRepository
import br.com.fatec.campusface.service.AuthCodeService
import br.com.fatec.campusface.service.AuthService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(ValidationController::class)
@AutoConfigureMockMvc(addFilters = false)
class ValidationControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var authCodeService: AuthCodeService

    // Mocks de infraestrutura necessários para o contexto subir
    @MockkBean
    lateinit var securityFilter: SecurityFilter
    @MockkBean
    lateinit var userRepository: UserRepository
    @MockkBean
    lateinit var authService: AuthService

    // --- TESTES DE GERAÇÃO ---

    @Test
    fun `generateQrCode deve retornar 200 OK com o codigo gerado`() {
        val orgId = "org123"
        val userId = "user123"
        val request = GenerateCodeRequest(organizationId = orgId)
        val responseMock = GeneratedCodeResponse(code = "123456", expirationTime = Instant.now())

        val userMock = User(id = userId, email = "teste@teste.com")
        val auth = UsernamePasswordAuthenticationToken(userMock, null, emptyList())

        every { authCodeService.generateCode(userId, orgId) } returns responseMock

        mockMvc.perform(post("/validate/qr-code/generate")
            .principal(auth) // <--- AQUI ESTÁ A CORREÇÃO
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.code").value("123456"))
    }

    @Test
    fun `generateQrCode deve retornar 400 Bad Request se service falhar`() {
        val request = GenerateCodeRequest(organizationId = "org123")
        val auth = UsernamePasswordAuthenticationToken(User(id = "u1"), null, emptyList())

        every { authCodeService.generateCode(any(), any()) } throws IllegalArgumentException("Erro de teste")

        mockMvc.perform(post("/validate/qr-code/generate")
            .principal(auth) // <--- CORREÇÃO
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    // --- TESTES DE VALIDAÇÃO ---

    @Test
    fun `validateQrCode deve retornar 200 OK quando codigo for valido`() {
        val codeStr = "654321"
        val fiscalId = "fiscal1"
        val request = ValidateCodeRequest(code = codeStr)
        val responseDto = ValidationResponseDTO(valid = true, message = "Sucesso", member = null)

        val auth = UsernamePasswordAuthenticationToken(User(id = fiscalId), null, emptyList())

        every { authCodeService.validateCode(codeStr, fiscalId) } returns responseDto

        mockMvc.perform(post("/validate/qr-code")
            .principal(auth) // <--- CORREÇÃO
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.valid").value(true))
    }

    @Test
    fun `validateQrCode deve retornar 403 Forbidden se fiscal nao tiver permissao`() {
        val request = ValidateCodeRequest(code = "123456")
        val auth = UsernamePasswordAuthenticationToken(User(id = "intruso"), null, emptyList())

        every { authCodeService.validateCode(any(), any()) } throws IllegalAccessException("Sem permissão")

        mockMvc.perform(post("/validate/qr-code")
            .principal(auth) // <--- CORREÇÃO
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `validateQrCode deve retornar 422 Unprocessable Entity se o service retornar valid=false`() {
        // ARRANGE
        val request = ValidateCodeRequest(code = "999999")

        // --- PRECISA DEFINIR O AUTH ---
        val auth = UsernamePasswordAuthenticationToken(User(id = "fiscal"), null, emptyList())

        val responseSoftFail = ValidationResponseDTO(
            valid = false,
            message = "Código expirado ou inválido.",
            member = null
        )

        every { authCodeService.validateCode(any(), any()) } returns responseSoftFail

        // ACT & ASSERT
        mockMvc.perform(post("/validate/qr-code")
            .principal(auth) // <--- ADICIONE ESTA LINHA
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `validateQrCode deve retornar 400 Bad Request se ocorrer erro generico no service`() {
        // ARRANGE
        val request = ValidateCodeRequest(code = "123456")

        // --- PRECISA DEFINIR O AUTH ---
        val auth = UsernamePasswordAuthenticationToken(User(id = "fiscal"), null, emptyList())

        every { authCodeService.validateCode(any(), any()) } throws RuntimeException("Erro inesperado no banco")

        // ACT & ASSERT
        mockMvc.perform(post("/validate/qr-code")
            .principal(auth) // <--- ADICIONE ESTA LINHA
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }
}