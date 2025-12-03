package br.com.fatec.campusface.controller

import br.com.fatec.campusface.dto.ApiResponse
import br.com.fatec.campusface.dto.MemberUpdateDTO
import br.com.fatec.campusface.dto.OrganizationMemberDTO
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import br.com.fatec.campusface.service.OrganizationMemberService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/members")
@SecurityRequirement(name = "bearerAuth")
class OrganizationMemberController(
    private val memberService: OrganizationMemberService,
    private val memberRepository: OrganizationMemberRepository // Usado para checar permissões
) {

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Lista membros de uma organização", description = "Opcionalmente filtre por cargo usando ?role=MEMBER")
    fun listMembers(
        @PathVariable organizationId: String,
        @RequestParam(required = false) role: Role?,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<List<OrganizationMemberDTO>>> {
        val user = authentication.principal as User

        // Verificação de Segurança
        if (!hasPermission(user.id, organizationId, listOf(Role.ADMIN, Role.VALIDATOR))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = "Você não tem permissão para visualizar membros desta organização.", data = null))
        }

        // Passa o filtro para o service
        val members = memberService.getAllMembers(organizationId, role)

        return ResponseEntity.ok(ApiResponse(success = true, message = "Membros listados.", data = members))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um membro específico pelo ID do vínculo")
    fun getMember(@PathVariable id: String, authentication: Authentication): ResponseEntity<ApiResponse<OrganizationMemberDTO>> {
        val user = authentication.principal as User
        val member = memberService.getMemberById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Membro não encontrado.", data = null))

        // Verificação de Segurança (precisamos saber a org do membro alvo primeiro)
        // Permite que o próprio usuário veja seus dados ou um Admin/Validator da org
        val isSelf = member.user.id == user.id
        if (!isSelf && !hasPermission(user.id, authenticationOrgId(id), listOf(Role.ADMIN, Role.VALIDATOR))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = "Acesso negado.", data = null))
        }

        return ResponseEntity.ok(ApiResponse(success = true, message = "Membro encontrado.", data = member))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza Role ou Status de um membro", description = "Requer permissão de ADMIN. Use para promover membros ou banir/ativar acesso.")
    fun updateMember(
        @PathVariable id: String,
        @RequestBody dto: MemberUpdateDTO,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<OrganizationMemberDTO>> {
        val user = authentication.principal as User

        // 1. Busca o membro alvo para saber de qual organização ele é
        val targetMember = memberRepository.findById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Membro não encontrado.", data = null))

        // 2. Verifica se quem está pedindo é ADMIN daquela organização
        if (!hasPermission(user.id, targetMember.organizationId, listOf(Role.ADMIN))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = "Apenas ADMINs podem gerenciar membros.", data = null))
        }

        // 3. Impede que o Admin remova seu próprio acesso de Admin (opcional, mas recomendado)
        if (targetMember.userId == user.id && dto.role != Role.ADMIN && dto.role != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse(success = false, message = "Você não pode rebaixar seu próprio cargo de ADMIN.", data = null))
        }

        var updatedMemberDto: OrganizationMemberDTO? = null

        // Aplica atualizações
        if (dto.role != null) {
            updatedMemberDto = memberService.updateMemberRole(id, dto.role)
        }
        if (dto.status != null) {
            updatedMemberDto = memberService.updateMemberStatus(id, dto.status)
        }

        // Se nada foi enviado no JSON
        if (updatedMemberDto == null) {
            updatedMemberDto = memberService.getMemberById(id)
        }

        return ResponseEntity.ok(ApiResponse(success = true, message = "Membro atualizado.", data = updatedMemberDto))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um membro da organização", description = "Requer permissão de ADMIN.")
    fun removeMember(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Void>> {
        val user = authentication.principal as User

        val targetMember = memberRepository.findById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse(success = false, message = "Membro não encontrado.", data = null))

        if (!hasPermission(user.id, targetMember.organizationId, listOf(Role.ADMIN))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse(success = false, message = "Apenas ADMINs podem remover membros.", data = null))
        }

        memberService.removeMember(id)

        return ResponseEntity.ok(ApiResponse(success = true, message = "Membro removido da organização.", data = null))
    }

    // --- Métodos Privados de Auxílio à Segurança ---

    /**
     * Verifica se o usuário (requesterId) possui uma das roles permitidas na organização alvo.
     */
    private fun hasPermission(requesterId: String, organizationId: String, allowedRoles: List<Role>): Boolean {
        val membership = memberRepository.findByUserIdAndOrganizationId(requesterId, organizationId)
        return membership != null && allowedRoles.contains(membership.role) && membership.status == br.com.fatec.campusface.models.MemberStatus.ACTIVE
    }

    /**
     * Recupera o ID da organização de um membro pelo ID do membro.
     * Útil quando só temos o ID do membro na URL.
     */
    private fun authenticationOrgId(memberId: String): String {
        return memberRepository.findById(memberId)?.organizationId ?: ""
    }
}