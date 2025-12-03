package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.*
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

    @GetMapping("/codes")
    @Operation(summary = "Listar códigos", description = "Lista todos os QR Codes gerados (Admin).")
    fun listCodes(): ResponseEntity<ApiResponse<List<AuthCodeResponseDTO>>> {
        val codes = authCodeService.listAllCodes()
        return ResponseEntity.ok(ApiResponse(success = true, message = "Códigos listados.", data = codes))
    }

    @GetMapping("/codes/{id}")
    @Operation(summary = "Buscar código por ID", description = "Detalhes de um QR Code específico.")
    fun getCode(@PathVariable id: String): ResponseEntity<ApiResponse<AuthCodeResponseDTO>> {
        val code = authCodeService.getCodeById(id)
        return if (code != null) {
            ResponseEntity.ok(ApiResponse(success = true, message = "Código encontrado.", data = code))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Código não encontrado.", data = null))
        }
    }

    @PutMapping("/codes/{id}")
    @Operation(summary = "Atualizar código", description = "Atualiza validade ou status manualmente.")
    fun updateCode(
        @PathVariable id: String,
        @RequestBody dto: AuthCodeUpdateDTO
    ): ResponseEntity<ApiResponse<AuthCodeResponseDTO>> {
        return try {
            val updated = authCodeService.updateCode(id, dto)
            ResponseEntity.ok(ApiResponse(success = true, message = "Código atualizado.", data = updated))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }

    @DeleteMapping("/codes/{id}")
    @Operation(summary = "Deletar código", description = "Remove o registro do QR Code.")
    fun deleteCode(@PathVariable id: String): ResponseEntity<ApiResponse<Void>> {
        return try {
            authCodeService.deleteCode(id)
            ResponseEntity.ok(ApiResponse(success = true, message = "Código removido.", data = null))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }

    @PostMapping("/codes/{id}/invalidate")
    @Operation(summary = "Invalidar código", description = "Invalida um código manualmente.")
    fun invalidateCode(@PathVariable id: String): ResponseEntity<ApiResponse<Void>> {
        return try {
            authCodeService.invalidateCodeManual(id)
            ResponseEntity.ok(ApiResponse(success = true, message = "Código invalidado.", data = null))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }
}