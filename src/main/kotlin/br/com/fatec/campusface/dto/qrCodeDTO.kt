package br.com.fatec.campusface.dto
import java.time.Instant
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class GenerateCodeRequest(
    @field:NotBlank(message = "O ID da organizacão nao pode ser nulo")
    val organizationId: String
)

data class GeneratedCodeResponse(
    val code: String,
    val expirationTime: Instant
)

data class ValidateCodeRequest(
    @field:NotBlank(message = "O código é obrigatório")
    @field:Size(min = 6, max = 6, message = "O código deve ter exatos 6 caracteres")
    val code: String
)

data class ValidationResponseDTO(
    val valid: Boolean,
    val message: String,
    val member: OrganizationMemberDTO?
)


data class AuthCodeResponseDTO(
    val id: String,
    val code: String,
    val userId: String,
    val organizationId: String,
    val expirationTime: Instant,
    val valid: Boolean
)

data class AuthCodeUpdateDTO(
    val valid: Boolean?,
    val expirationTime: Instant?
)