package br.com.fatec.campusface.dto

import br.com.fatec.campusface.models.RequestStatus
import br.com.fatec.campusface.models.Role
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class EntryRequestCreateDTO(
    @field:NotBlank(message = "O código do Hub é obrigatório")
    val hubCode: String,

    @field:NotNull(message = "O papel (Role) é obrigatório")
    val role: Role
)

data class EntryRequestResponseDTO(
    val id: String,
    val hubCode: String,
    val role: Role,
    val status: RequestStatus,
    val requestedAt: Instant, // ou Instant
    val user: UserDTO // Dados completos do solicitante para o Admin ver a foto
)