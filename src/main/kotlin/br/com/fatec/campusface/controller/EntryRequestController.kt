package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.ApiResponse
import br.com.fatec.campusface.dto.EntryRequestCreateDTO
import br.com.fatec.campusface.dto.EntryRequestResponseDTO
import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.models.User
import org.springframework.security.access.prepost.PreAuthorize
import br.com.fatec.campusface.service.EntryRequestService
import br.com.fatec.campusface.service.UserService
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
    private val userService: UserService
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


    /**
     * Lista todas as solicitações pendentes de um Hub específico.
     * Acessível para Admins do Hub.
     */
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
        }catch (e:Exception){
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
        }catch (e: Exception){
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(success = false, message ="Erro ao rejeitar ${e.message}", data = null)
            )
        }
    }


    @GetMapping("/whoami")
    fun whoAmI(authentication: Authentication): ResponseEntity<ApiResponse<Any>> {
        try {

            println("TESTE whoAmI $authentication")

            val userModel = authentication.principal as br.com.fatec.campusface.models.User

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
    @Operation(summary = "Lista minhas solicitações", description = "Retorna o histórico de solicitações de entrada do usuário logado.")
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
}
