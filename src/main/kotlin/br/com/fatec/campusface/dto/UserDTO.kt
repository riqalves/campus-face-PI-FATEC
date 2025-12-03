package br.com.fatec.campusface.dto
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.models.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant


data class UserDTO(
    val id: String,
    val fullName: String,
    val email: String,
    val document: String?,
    val faceImageId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(user: User, signedFaceUrl: String?) = UserDTO(
            id = user.id,
            fullName = user.fullName,
            email = user.email,
            document = user.document,

            // ERRADO (O que deve estar agora):
            // faceImageId = user.faceImageId ?: "default.png",

            // CORRETO (Use a URL assinada se ela existir):
            faceImageId = signedFaceUrl ?: user.faceImageId ?: "default.png",

            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }
}


data class UserUpdateDTO(
    @field:Size(min = 3, message = "O nome deve ter no mínimo 3 caracteres")
    val fullName: String?,

    @field:Email(message = "Formato de email inválido")
    val email: String?,

    @field:Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    val password: String?,

    val document: String?,

)


data class ApiResponse<T>(
    val message: String?,
    val success: Boolean,
    val data: T? = null
)



data class LoginDTO(
    @field:NotBlank(message = "O email é obrigatório")
    @field:Email(message = "Formato de email inválido")
    val email: String,

    @field:NotBlank(message = "A senha é obrigatória")
    val password: String
)

