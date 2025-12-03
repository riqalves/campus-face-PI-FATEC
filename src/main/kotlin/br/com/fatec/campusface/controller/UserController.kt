package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.ApiResponse
import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.dto.UserUpdateDTO
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Controller", description = "Endpoints para gerenciamento do perfil do usuário ")
class UserController(private val userService: UserService) {

    @GetMapping("/{id}")
    @Operation(
        summary = "Busca dados do usuário",
        description = "Retorna os detalhes do perfil. O usuário só pode visualizar o seu próprio perfil."
    )
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Usuário encontrado"),
        SwaggerApiResponse(responseCode = "403", description = "Acesso negado (tentativa de ver dados de outro usuário)"),
        SwaggerApiResponse(responseCode = "404", description = "Usuário não encontrado")
    ])
    fun getUser(
        @Parameter(description = "ID do usuário a ser buscado") @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<UserDTO>> {
        val currentUser = authentication.principal as User
        println("DEBUG [getUser] $id, $currentUser")

        // Regra de Segurança: Apenas o próprio dono dos dados pode vê-los
        if (currentUser.id != id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = "Acesso negado aos dados de outro usuário.", data = null))
        }

        val userDto = userService.getUserById(id)
        return if (userDto != null) {
            ResponseEntity.ok(ApiResponse(success = true, message = "Usuário encontrado.", data = userDto))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Usuário não encontrado.", data = null))
        }
    }

    /**
     * Endpoint para atualizar apenas a foto de perfil.
     */
    @PatchMapping(value = ["/image"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Atualiza a foto de perfil",
        description = "Permite que o usuário troque sua foto facial. Requer envio via Multipart Form-Data."
    )
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Foto atualizada com sucesso"),
        SwaggerApiResponse(responseCode = "400", description = "Erro no upload ou arquivo inválido"),
        SwaggerApiResponse(responseCode = "403", description = "Acesso negado")
    ])
    fun updateProfileImage(
        @Parameter(description = "Arquivo de imagem (JPG/PNG)", required = true) @RequestParam("image") image: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<UserDTO>> {
        val currentUser = authentication.principal as User
        val id = currentUser.id

        return try {
            val updatedUser = userService.updateProfileImage(id, image)

            // TODO: Se esse usuário já for membro de organizações, disparar ChangeRequests ou Syncs.
            // Por enquanto, apenas atualiza o perfil base.

            ResponseEntity.ok(
                ApiResponse(success = true, message = "Foto de perfil atualizada com sucesso!", data = updatedUser)
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Exclui a conta do usuário",
        description = "Ação irreversível. Remove o usuário, sua foto e seus dados do sistema."
    )
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Conta excluída com sucesso"),
        SwaggerApiResponse(responseCode = "403", description = "Acesso negado"),
        SwaggerApiResponse(responseCode = "404", description = "Usuário não encontrado")
    ])
    fun deleteUser(
        @Parameter(description = "ID do usuário a ser excluído") @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Void>> {
        val currentUser = authentication.principal as User

        if (currentUser.id != id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = "Você não pode excluir a conta de outro usuário.", data = null))
        }

        val deleted = userService.deleteUser(id)
        return if (deleted) {
            ResponseEntity.ok(ApiResponse(success = true, message = "Conta excluída com sucesso.", data = null))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Usuário não encontrado.", data = null))
        }
    }

    @GetMapping("/allusers")
    @Operation(
        summary = "Listar todos os usuários",
        description = "Retorna uma lista com todos os usuários cadastrados no sistema."
    )
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Lista recuperada com sucesso")
    ])
    fun getAllUsers(): ResponseEntity<ApiResponse<List<UserDTO>>> {
        val users = userService.listUsers()

        return ResponseEntity.ok().body(ApiResponse(success = true, message = "aqui esta os usuarios", data = users))
    }

    @PutMapping()
    @Operation(
        summary = "Atualiza dados cadastrais",
        description = "Atualiza nome, email, senha e documento. Campos opcionais."
    )
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Dados atualizados com sucesso"),
        SwaggerApiResponse(responseCode = "400", description = "Dados inválidos ou email já em uso"),
        SwaggerApiResponse(responseCode = "403", description = "Acesso negado")
    ])
    fun updateUser(
        @Valid @RequestBody data: UserUpdateDTO,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<UserDTO>> {
        println("DEBUG RECEBIDO: Nome=${data.fullName}, Email=${data.email}")
        //TODO corrigir adicao ao inves de update
        val currentUser = authentication.principal as User
        println("DEBUG UPDATE $currentUser, $data")
        // só o próprio usuário pode se atualizar
        val id = currentUser.id

        return try {
            val updatedUser = userService.updateUser(id, data)

            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Dados atualizados com sucesso.",
                    data = updatedUser
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }
}