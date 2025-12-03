package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.ApiResponse
import br.com.fatec.campusface.dto.ClientCheckinDTO
import br.com.fatec.campusface.dto.RegisteredClientResponseDTO
import br.com.fatec.campusface.service.RegisteredClientService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/clients")
@Tag(name = "Client Check-in", description = "Endpoints para registro dos Totens Python")
class RegisteredClientController(
    private val clientService: RegisteredClientService
) {

    @PostMapping("/checkin")
    @Operation(summary = "Check-in do Totem", description = "O Python chama isso ao ligar. Requer Token Bearer no Header e HubCode no Body.")
    fun checkin(
        @Valid @RequestBody data: ClientCheckinDTO,

        @Parameter(
            name = "Authorization",
            description = "Token Bearer do Validador",
            required = true,
            `in` = ParameterIn.HEADER,
            schema = Schema(type = "string")
        )
        @RequestHeader("Authorization") tokenHeader: String
    ): ResponseEntity<ApiResponse<RegisteredClientResponseDTO>> {
        return try {

            println("DEBUG: ENDPOINT CHECKIN: $data")
            val client = clientService.processCheckin(data, tokenHeader)

            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Check-in realizado com sucesso.",
                    data = client
                )
            )
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = e.message, data = null))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = e.message, data = null))
        }
    }
}