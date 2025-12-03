package br.com.fatec.campusface.service

import br.com.fatec.campusface.dto.OrganizationMemberDTO
import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.models.MemberStatus
import br.com.fatec.campusface.models.OrganizationMember
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import br.com.fatec.campusface.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class OrganizationMemberService(
    private val memberRepository: OrganizationMemberRepository,
    private val userRepository: UserRepository,
    private val cloudinaryService: CloudinaryService,
    private val syncService: SyncService
) {

    /**
     * Lista membros de uma organização, opcionalmente filtrando por Role.
     */
    fun getAllMembers(organizationId: String, role: Role? = null): List<OrganizationMemberDTO> {
        val members = if (role != null) {
            memberRepository.findByOrganizationIdAndRole(organizationId, role)
        } else {
            memberRepository.findAllByOrganizationId(organizationId)
        }

        // busca todos os usuários de uma vez
        if (members.isEmpty()) return emptyList()

        val userIds = members.map { it.userId }.distinct()
        val usersMap = userRepository.findAllByIds(userIds).associateBy { it.id }

        return members.mapNotNull { member ->
            val user = usersMap[member.userId]
            user?.let { toMemberDTO(member, it) }
        }
    }

    /**
     * Busca um membro específico pelo ID do vínculo (organizationMemberId).
     */
    fun getMemberById(id: String): OrganizationMemberDTO? {
        val member = memberRepository.findById(id) ?: return null
        val user = userRepository.findById(member.userId) ?: return null
        return toMemberDTO(member, user)
    }

    /**
     * Atualiza o papel (Role) de um membro (ex: MEMBER -> VALIDATOR).
     */
    fun updateMemberRole(id: String, newRole: Role): OrganizationMemberDTO {
        val member = memberRepository.findById(id)
            ?: throw IllegalArgumentException("Membro não encontrado")

        if (member.role == newRole) return getMemberById(id)!!

        memberRepository.updateRole(id, newRole)

        // TODO: Chamar SyncService para atualizar permissões nos totens se necessário
        // ex: syncService.notifyMemberUpdate(member.organizationId, member.userId)
        println("TODO: SyncService - Role do membro ${member.id} atualizada para $newRole")

        return getMemberById(id)!!
    }

    /**
     * Atualiza o Status de um membro (ex: ACTIVE -> INACTIVE).
     * Se inativar, o totem deve bloquear o acesso imediatamente.
     */
    fun updateMemberStatus(id: String, newStatus: MemberStatus): OrganizationMemberDTO {
        val member = memberRepository.findById(id)
            ?: throw IllegalArgumentException("Membro não encontrado")

        if (member.status == newStatus) return getMemberById(id)!!

        memberRepository.updateStatus(id, newStatus)

        // TODO: Chamar SyncService. Se status != ACTIVE, o Python deve remover a face da memória.
        println("TODO: SyncService - Status do membro ${member.id} atualizado para $newStatus")

        return getMemberById(id)!!
    }

    /**
     * Remove um membro da organização.
     * Isso deve remover a face dos totens Python imediatamente.
     */
    fun removeMember(id: String) {
        val member = memberRepository.findById(id)
            ?: throw IllegalArgumentException("Membro não encontrado")

        // Salva os IDs antes de deletar para poder notificar
        val orgId = member.organizationId
        val userId = member.userId

        memberRepository.delete(id)

        // GATILHO DE SYNC (DELETE)
        syncService.syncMemberDeletion(orgId, userId)

        println("INFO: Membro removido e disparo de sync enviado.")
    }

    // --- Métodos Auxiliares ---

    /**
     * Converte Entity -> DTO.
     * Gera a URL assinada temporária para a foto do usuário.
     * Prioriza a foto do Member (se houver), senão usa a do User.
     */
    private fun toMemberDTO(member: OrganizationMember, user: User): OrganizationMemberDTO {
        // Lógica de Foto: O membro pode ter uma foto específica para aquela org?
        // Por enquanto assumimos que usamos o faceImageId do User, mas se o member tiver override, usamos ele.
        val faceId = member.faceImageId?.ifBlank { null } ?: user.faceImageId

        val signedUrl = faceId?.let { cloudinaryService.generateSignedUrl(it) }

        // Cria o UserDTO
        val userDTO = UserDTO.fromEntity(user, signedUrl)

        return OrganizationMemberDTO(
            id = member.id,
            role = member.role,
            status = member.status,
            joinedAt = member.createdAt,
            user = userDTO
        )
    }
}