package br.com.fatec.campusface.service

import br.com.fatec.campusface.dto.EntryRequestCreateDTO
import br.com.fatec.campusface.dto.EntryRequestResponseDTO
import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.models.EntryRequest
import br.com.fatec.campusface.models.MemberStatus
import br.com.fatec.campusface.models.OrganizationMember
import br.com.fatec.campusface.models.RequestStatus
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.repository.UserRepository
import br.com.fatec.campusface.repository.EntryRequestRepository
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import br.com.fatec.campusface.repository.OrganizationRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class EntryRequestService(
    private val entryRequestRepository: EntryRequestRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val cloudinaryService: CloudinaryService,
    private val syncService: SyncService
) {

    /**
     * Cria um novo pedido de entrada para uma organização baseada no Código do Hub.
     */
    fun createRequest(userId: String, data: EntryRequestCreateDTO): EntryRequestResponseDTO {

        val organization = organizationRepository.findByHubCode(data.hubCode)
            ?: throw IllegalArgumentException("Organização não encontrada com o código: ${data.hubCode}")

        val existingMember = organizationMemberRepository.findByUserIdAndOrganizationId(userId, organization.id)
        if (existingMember != null) {
            throw IllegalStateException("Você já é um membro desta Organização.")
        }

        // verifica se já existe um pedido PENDENTE
        val pendingRequests = entryRequestRepository.findByOrganizationIdAndStatus(organization.id, RequestStatus.PENDING)
        if (pendingRequests.any { it.userId == userId }) {
            throw IllegalStateException("Você já possui uma solicitação pendente para esta organização.")
        }

        val newEntryRequest = EntryRequest(
            userId = userId,
            organizationId = organization.id,
            hubCode = organization.hubCode,
            role = data.role, // O usuário solicita o cargo (ex: MEMBER), mas o Admin pode mudar depois
            status = RequestStatus.PENDING,
            requestedAt = Instant.now()
        )

        val savedRequest = entryRequestRepository.save(newEntryRequest)

        val user = userRepository.findById(userId) ?: throw IllegalStateException("Usuário não encontrado no banco de dados.")
        return toResponseDTO(savedRequest, user)
    }

    /**
     * Lista pedidos pendentes para um Hub.
     * Usado pelo App do Admin para saber quem quer entrar.
     */
    fun listPendingRequests(hubCode: String): List<EntryRequestResponseDTO> {
        val organization = organizationRepository.findByHubCode(hubCode)
            ?: throw IllegalArgumentException("Hub não encontrado")

        val requests = entryRequestRepository.findByOrganizationIdAndStatus(organization.id, RequestStatus.PENDING)

        return requests.mapNotNull { req ->
            val user = userRepository.findById(req.userId)
            user?.let { toResponseDTO(req, it) }
        }
    }

    /**
     * Lista o histórico de solicitações do próprio usuário.
     */
    fun listUserRequests(userId: String): List<EntryRequestResponseDTO> {
        val requests = entryRequestRepository.findByUserId(userId)

        val user = userRepository.findById(userId)
            ?: throw IllegalStateException("Usuário não encontrado")

        return requests.map { req ->
            toResponseDTO(req, user)
        }
    }

    /**
     * Aprova uma solicitação:
     * 1. Muda status da request para APPROVED.
     * 2. Cria o registro oficial em OrganizationMember.
     * 3. Dispara sincronização para os totens.
     */
    fun approveRequest(requestId: String) {
        val request = entryRequestRepository.findById(requestId)
            ?: throw IllegalArgumentException("Solicitação não encontrada")

        if (request.status != RequestStatus.PENDING) {
            throw IllegalStateException("Esta solicitação já foi processada.")
        }

        // Cria o vinculo no OrganizationMember
        val newMember = OrganizationMember(
            organizationId = request.organizationId,
            userId = request.userId,
            role = request.role,
            status = MemberStatus.ACTIVE,
            faceImageId = null // Usa a foto do perfil do usuário (User)
        )
        organizationMemberRepository.save(newMember)

        // atualiza listas na Organization
        when (newMember.role) {
            Role.MEMBER -> organizationRepository.addMemberToOrganization(newMember.organizationId, newMember.userId)
            Role.VALIDATOR -> organizationRepository.addValidatorToOrganization(newMember.organizationId, newMember.userId)
            Role.ADMIN -> organizationRepository.addAdminToOrganization(newMember.organizationId, newMember.userId)
        }

        entryRequestRepository.updateStatus(request.id, RequestStatus.APPROVED)

        // Dispara o Upsert. O Python vai receber a foto e criar o registro no banco vetorial.
        syncService.syncNewMember(newMember.organizationId, newMember.userId)
    }

    /**
     * Rejeita uma solicitação.
     */
    fun rejectRequest(requestId: String) {
        val request = entryRequestRepository.findById(requestId)
            ?: throw IllegalArgumentException("Solicitação não encontrada")

        if (request.status != RequestStatus.PENDING) {
            throw IllegalStateException("Solicitação não está pendente.")
        }

        entryRequestRepository.updateStatus(requestId, RequestStatus.DENIED)
    }

    // --- Auxiliar ---

    private fun toResponseDTO(entryRequest: EntryRequest, user: User): EntryRequestResponseDTO {
        // Gera URL assinada para o admin ver a cara do sujeito antes de aprovar
        val faceUrl = user.faceImageId?.let { cloudinaryService.generateSignedUrl(it) }
        val userDTO = UserDTO.fromEntity(user, faceUrl)

        return EntryRequestResponseDTO(
            id = entryRequest.id,
            hubCode = entryRequest.hubCode,
            role = entryRequest.role,
            status = entryRequest.status,
            requestedAt = entryRequest.requestedAt,
            user = userDTO,
        )
    }
}