package br.com.fatec.campusface.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import br.com.fatec.campusface.models.MemberStatus
import br.com.fatec.campusface.models.Role
import java.time.Instant

data class OrganizationMemberDTO(
    val id: String,
    val role: Role,
    val status: MemberStatus,
    val joinedAt: Instant,
    val user: UserDTO
)

data class MemberUpdateDTO(
    val role: Role?,
    val status: MemberStatus?,
)


// DTO para criar um membro diretamente (sem passar por aprovação)
data class OrganizationMemberCreateDTO(
    @field:NotBlank(message = "O ID do usuário é obrigatório")
    val userId: String,

    @field:NotBlank(message = "O ID da organização é obrigatório")
    val organizationId: String,

    @field:NotNull(message = "O papel (Role) é obrigatório")
    val role: Role
)