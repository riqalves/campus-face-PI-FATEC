package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.*
import br.com.fatec.campusface.models.User
import org.springframework.security.access.prepost.PreAuthorize
import br.com.fatec.campusface.service.EntryRequestService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/entry-requests")
@SecurityRequirement(name = "bearerAuth")
class EntryRequestController(
    private val entryRequestService: EntryRequestService,
) {

    @PostMapping("/create")
    fun createRequest(
        @Valid @RequestBody data: EntryRequestCreateDTO,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<EntryRequestResponseDTO>> {
        return try {
            println("DEBUG - EntryRequestController $data")
            val user = authentication.principal as User
            val request = entryRequestService.createRequest(user.id, data)

            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    ApiResponse(
                        success = true,
                        message = "Solicitação enviada com sucesso. Aguarde a aprovação.",
                        data = request
                    )
                )
        } catch (e: IllegalArgumentException) {
            println("ERRO - Falha ao criar solicitação: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    ApiResponse(
                        success = false,
                        message = "Erro ao criar pedido: ${e.message}",
                        data = null
                    )
                )
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(success = false, message = "Erro ao criar solicitação: ${e.message}", data = null)
            )
        }
    }


    @GetMapping("/organization/{hubCode}")
    @PreAuthorize("isAuthenticated()")
    fun listPendingRequests(@PathVariable hubCode: String): ResponseEntity<ApiResponse<List<EntryRequestResponseDTO>>> {
        return try {
            // TODO: Idealmente, verificar aqui ou no serviço se o usuário logado é ADMIN deste hubCode
            val requests = entryRequestService.listPendingRequests(hubCode)

            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Solicitações pendentes encontradas.",
                    data = requests
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(success = false, message = e.message, data = null)
            )
        }
    }


    @PostMapping("/{requestId}/approve")
    fun approveRequest(@PathVariable requestId: String): ResponseEntity<ApiResponse<Void>> {
        return try {
            // TODO: Veririficar se o usuario logado e admin da org dessa requisicao
            entryRequestService.approveRequest(requestId)

            ResponseEntity.ok(
                ApiResponse(success = true, message = "Solicitacao aprovada com sucesso", data = null)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(success = false, message = "Erro ao aprovar: ${e.message}", data = null)
            )
        }
    }


    @PostMapping("/{requestId}/reject")
    @PreAuthorize("isAuthenticated()")
    fun rejectRequest(@PathVariable requestId: String): ResponseEntity<ApiResponse<Void>> {
        return try {
            //TODO: verificar se o usuario logado é admin da organizacao dessa request
            entryRequestService.rejectRequest(requestId)
            ResponseEntity.ok(
                ApiResponse(success = true, message = "Solicitacao rejeitada", data = null)
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(success = false, message = "Erro ao rejeitar ${e.message}", data = null)
            )
        }
    }


    @GetMapping("/whoami")
    fun whoAmI(authentication: Authentication): ResponseEntity<ApiResponse<Any>> {
        try {

            println("TESTE whoAmI $authentication")

            val userModel = authentication.principal as User

            val userResponseDto = UserDTO(
                id = userModel.id,
                fullName = userModel.fullName,
                email = userModel.email,
                document = userModel.document,
                faceImageId = userModel.faceImageId!!,
                createdAt = userModel.createdAt,
                updatedAt = userModel.updatedAt,
            )

            val authInfo = mapOf(
                "user" to userResponseDto, // Retornando o DTO formatado
                "authorities" to authentication.authorities.map { it.authority },
                "principalType" to authentication.principal.javaClass.name
            )

            return ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Dados do usuário autenticado.",
                    data = authInfo
                )
            )
        } catch (e: ClassCastException) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Erro de cast! O principal não é do tipo esperado. Tipo encontrado: ${authentication.principal.javaClass.name}. Erro: ${e.message}",
                    data = null
                )
            )
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse(
                    success = false,
                    message = "Erro ao recuperar dados de autenticação: ${e.message}",
                    data = null
                )
            )
        }
    }

    @GetMapping("/my-requests")
    @Operation(
        summary = "Lista minhas solicitações",
        description = "Retorna o histórico de solicitações de entrada do usuário logado."
    )
    fun listMyRequests(authentication: Authentication): ResponseEntity<ApiResponse<List<EntryRequestResponseDTO>>> {
        val user = authentication.principal as User

        val requests = entryRequestService.listUserRequests(user.id)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Histórico de solicitações recuperado.",
                data = requests
            )
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar solicitação por ID", description = "Detalhes de uma solicitação específica.")
    fun getRequest(@PathVariable id: String): ResponseEntity<ApiResponse<EntryRequestResponseDTO>> {
        val request = entryRequestService.getRequestById(id)
        return if (request != null) {
            ResponseEntity.ok(ApiResponse(success = true, message = "Solicitação encontrada.", data = request))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Solicitação não encontrada.", data = null))
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar solicitação",
        description = "Permite alterar o cargo solicitado (apenas se PENDING)."
    )
    fun updateRequest(
        @PathVariable id: String,
        @RequestBody data: EntryRequestUpdateDTO,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<EntryRequestResponseDTO>> {
        // TODO: Adicionar validação de segurança (se quem edita é o dono da request)
        return try {
            val updated = entryRequestService.updateRequest(id, data.role)
            ResponseEntity.ok(ApiResponse(success = true, message = "Solicitação atualizada.", data = updated))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar solicitação", description = "Cancela/Remove uma solicitação de entrada.")
    fun deleteRequest(@PathVariable id: String, authentication: Authentication): ResponseEntity<ApiResponse<Void>> {
        // TODO: adicionar validação de segurança (se quem deleta é o dono ou admin)
        return try {
            entryRequestService.deleteRequest(id)
            ResponseEntity.ok(ApiResponse(success = true, message = "Solicitação removida.", data = null))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }
}
