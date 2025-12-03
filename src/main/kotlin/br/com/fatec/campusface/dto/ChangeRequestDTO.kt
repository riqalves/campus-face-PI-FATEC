package br.com.fatec.campusface.dto


import br.com.fatec.campusface.models.RequestStatus
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class ChangeRequestCreateDTO(
    @field:NotBlank(message = "O ID da nova imagem é obrigatório")
    val newFaceImageId: String,

    @field:NotBlank(message = "O ID da organização é obrigatório")
    val organizationId: String
)

data class ChangeRequestResponseDTO(
    val id: String,
    val status: RequestStatus,
    val requestedAt: Instant,

    val organizationId: String,

    val userFullName: String,
    val currentFaceUrl: String?, // A foto que está valendo hoje
    val newFaceUrl: String       // A foto que ele quer colocar
)

data class ReviewRequestDTO(
    val approved: Boolean // true = APROVAR, false = REJEITAR
)