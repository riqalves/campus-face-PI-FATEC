package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.ApiResponse
import br.com.fatec.campusface.dto.GenerateCodeRequest
import br.com.fatec.campusface.dto.GeneratedCodeResponse
import br.com.fatec.campusface.dto.ValidateCodeRequest
import br.com.fatec.campusface.dto.ValidationResponseDTO
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.service.AuthCodeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/validate")
@SecurityRequirement(name = "bearerAuth")
class ValidationController(
    private val authCodeService: AuthCodeService
) {

    @PostMapping("/qr-code/generate")
    @Operation(summary = "Gera um QR Code para entrada", description = "O usuário deve especificar para qual organização quer entrar.")
    fun generateQrCode(
        @RequestBody request: GenerateCodeRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<GeneratedCodeResponse>> {
        val user = authentication.principal as User
        return try {
            val response = authCodeService.generateCode(user.id, request.organizationId)

            ResponseEntity.ok(ApiResponse(success = true, message = "Código gerado com sucesso.", data = response))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }

    @PostMapping("/qr-code")
    @Operation(summary = "Valida um QR Code (Uso do Fiscal)", description = "Retorna os dados do membro se o código for válido e o fiscal tiver permissão.")
    fun validateQrCode(
        @RequestBody request: ValidateCodeRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ValidationResponseDTO>> {
        val validator = authentication.principal as User
        return try {
            // Passamos o ID do fiscal para validar se ele pode fiscalizar aquela org
            val validationResult = authCodeService.validateCode(request.code, validator.id)

            if (validationResult.valid) {
                ResponseEntity.ok(ApiResponse(success = true, message = validationResult.message, data = validationResult))
            } else {
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse(success = false, message = validationResult.message, data = validationResult))
            }
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = e.message, data = null))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }
}