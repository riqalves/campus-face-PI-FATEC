package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.*
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.service.OrganizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/organizations")
@SecurityRequirement(name = "bearerAuth")
class OrganizationController(private val organizationService: OrganizationService) {

    @PostMapping
    @Operation(summary = "Cria uma nova Organização", description = "O usuário logado se torna automaticamente o ADMIN da organização.")
    fun create(
        @Valid @RequestBody orgData: OrganizationCreateDTO,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<OrganizationResponseDTO>> {
        return try {
            val user = authentication.principal as User

            // O service cria a Org e o OrganizationMember (ADMIN) para este usuário
            val createdOrgEntity = organizationService.createOrganization(orgData, user)

            // Buscamos o DTO completo (com a lista de admins populada) para retornar
            val orgDto = organizationService.getOrganizationById(createdOrgEntity.id)

            ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse(
                    success = true,
                    message = "Organização criada com sucesso.",
                    data = orgDto
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse(success = false, message = e.message, data = null)
            )
        }
    }

    @GetMapping
    @Operation(summary = "Lista todas as Organizações")
    fun listAll(): ResponseEntity<ApiResponse<List<OrganizationResponseDTO>>> {
        val orgs = organizationService.getAllOrganizations()
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Organizações recuperadas.",
                data = orgs
            )
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma Organização por ID")
    fun getById(@PathVariable id: String): ResponseEntity<ApiResponse<OrganizationResponseDTO>> {
        val org = organizationService.getOrganizationById(id)
        return if (org != null) {
            ResponseEntity.ok(ApiResponse(success = true, message = "Organização encontrada.", data = org))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Organização não encontrada.", data = null))
        }
    }

    @GetMapping("/hub/{hubCode}")
    @Operation(summary = "Busca por HubCode", description = "Endpoint essencial para a inicialização dos Totens Python.")
    fun getByHubCode(@PathVariable hubCode: String): ResponseEntity<ApiResponse<OrganizationResponseDTO>> {
        val org = organizationService.getOrganizationByHubCode(hubCode)
        return if (org != null) {
            ResponseEntity.ok(ApiResponse(success = true, message = "Organização encontrada.", data = org))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Organização não encontrada.", data = null))
        }
    }

    @GetMapping("/my-hubs")
    @Operation(summary = "Listar meus hubs", description = "Retorna todas as organizações das quais o usuário logado faz parte.")
    fun listMyHubs(authentication: Authentication): ResponseEntity<ApiResponse<List<OrganizationResponseDTO>>> {
        val user = authentication.principal as User

        val myHubs = organizationService.listUserHubs(user.id)

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Seus hubs foram recuperados.",
                data = myHubs
            )
        )
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados da Organização", description = "Requer que o usuário logado seja ADMIN desta organização.")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody dto: OrganizationUpdateDTO,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<OrganizationResponseDTO>> {
        val user = authentication.principal as User

        val existingOrg = organizationService.getOrganizationById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Organização não encontrada.", data = null))

        val isAdmin = existingOrg.admins.any { it.id == user.id }

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = "Você não tem permissão de ADMIN nesta organização.", data = null))
        }

        val updatedOrg = organizationService.updateOrganization(id, dto)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Organização atualizada.", data = updatedOrg))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma Organização", description = "Requer que o usuário logado seja ADMIN desta organização.")
    fun delete(@PathVariable id: String, authentication: Authentication): ResponseEntity<ApiResponse<Void>> {
        val user = authentication.principal as User

        val existingOrg = organizationService.getOrganizationById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Organização não encontrada.", data = null))

        val isAdmin = existingOrg.admins.any { it.id == user.id }

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = "Você não tem permissão de ADMIN nesta organização.", data = null))
        }

        organizationService.deleteOrganization(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Organização removida com sucesso.", data = null))
    }
}