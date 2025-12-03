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
}