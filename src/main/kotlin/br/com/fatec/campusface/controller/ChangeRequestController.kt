package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.ApiResponse
import br.com.fatec.campusface.dto.ChangeRequestResponseDTO
import br.com.fatec.campusface.dto.ReviewRequestDTO
import br.com.fatec.campusface.models.ChangeRequest
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.service.ChangeRequestService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/change-requests")
@SecurityRequirement(name = "bearerAuth")
class ChangeRequestController(
    private val changeRequestService: ChangeRequestService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Solicitar troca de foto", description = "Usuário envia uma nova foto para análise do Admin.")
    fun createRequest(
        @RequestParam("organizationId") organizationId: String,
        @RequestParam("image") image: MultipartFile,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ChangeRequest>> {
        val user = authentication.principal as User
        return try {
            val request = changeRequestService.createRequest(user.id, organizationId, image)
            ResponseEntity.ok(ApiResponse(success = true, message = "Solicitação enviada.", data = request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Listar pendências", description = "Admin lista solicitações de troca pendentes.")
    fun listPending(
        @PathVariable organizationId: String,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<List<ChangeRequestResponseDTO>>> {
        // TODO: Validar se user é admin
        val requests = changeRequestService.listPendingRequests(organizationId)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Lista recuperada.", data = requests))
    }

    @PostMapping("/{requestId}/review")
    @Operation(summary = "Revisar solicitação", description = "Aprovar ou Rejeitar a troca de foto.")
    fun reviewRequest(
        @PathVariable requestId: String,
        @RequestBody body: ReviewRequestDTO,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Void>> {
        val admin = authentication.principal as User
        return try {
            changeRequestService.reviewRequest(requestId, admin.id, body.approved)
            val msg = if (body.approved) "Aprovado com sucesso." else "Rejeitado."
            ResponseEntity.ok(ApiResponse(success = true, message = msg, data = null))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Meus pedidos", description = "Lista histórico de trocas de foto do usuário logado.")
    fun listMyRequests(authentication: Authentication): ResponseEntity<ApiResponse<List<ChangeRequestResponseDTO>>> {
        val user = authentication.principal as User
        val requests = changeRequestService.listUserRequests(user.id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Histórico recuperado.", data = requests))
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Buscar por ID", description = "Detalhes de uma solicitação específica.")
    fun getRequest(@PathVariable requestId: String): ResponseEntity<ApiResponse<ChangeRequestResponseDTO>> {
        val request = changeRequestService.getRequestById(requestId)
        return if (request != null) {
            ResponseEntity.ok(ApiResponse(success = true, message = "Encontrado.", data = request))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Não encontrado.", data = null))
        }
    }

    @DeleteMapping("/{requestId}")
    @Operation(summary = "Deletar solicitação", description = "Cancela/Remove uma solicitação.")
    fun deleteRequest(@PathVariable requestId: String): ResponseEntity<ApiResponse<Void>> {
        return try {
            changeRequestService.deleteRequest(requestId)
            ResponseEntity.ok(ApiResponse(success = true, message = "Removido com sucesso.", data = null))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }

    @PutMapping(value = ["/{requestId}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Atualizar solicitação", description = "Permite reenviar a foto (apenas se PENDING).")
    fun updateRequest(
        @PathVariable requestId: String,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<ApiResponse<ChangeRequest>> {
        return try {
            val updated = changeRequestService.updateRequest(requestId, image)
            ResponseEntity.ok(ApiResponse(success = true, message = "Atualizado.", data = updated))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }
}