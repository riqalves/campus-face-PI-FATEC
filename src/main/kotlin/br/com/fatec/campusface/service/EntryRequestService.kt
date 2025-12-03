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

    fun createRequest(userId: String, data: EntryRequestCreateDTO): EntryRequestResponseDTO {

        val organization = organizationRepository.findByHubCode(data.hubCode)
            ?: throw IllegalArgumentException("Organização não encontrada com o código: ${data.hubCode}")

        val existingMember = organizationMemberRepository.findByUserIdAndOrganizationId(userId, organization.id)
        if (existingMember != null) {
            throw IllegalStateException("Você já é um membro desta Organização.")
        }

        val pendingRequests = entryRequestRepository.findByOrganizationIdAndStatus(organization.id, RequestStatus.PENDING)
        if (pendingRequests.any { it.userId == userId }) {
            throw IllegalStateException("Você já possui uma solicitação pendente para esta organização.")
        }

        val newEntryRequest = EntryRequest(
            userId = userId,
            organizationId = organization.id,
            hubCode = organization.hubCode,
            role = data.role,
            status = RequestStatus.PENDING,
            requestedAt = Instant.now()
        )

        val savedRequest = entryRequestRepository.save(newEntryRequest)

        val user = userRepository.findById(userId) ?: throw IllegalStateException("Usuário não encontrado no banco de dados.")
        return toResponseDTO(savedRequest, user)
    }

    fun listPendingRequests(hubCode: String): List<EntryRequestResponseDTO> {
        val organization = organizationRepository.findByHubCode(hubCode)
            ?: throw IllegalArgumentException("Hub não encontrado")

        val requests = entryRequestRepository.findByOrganizationIdAndStatus(organization.id, RequestStatus.PENDING)

        return requests.mapNotNull { req ->
            val user = userRepository.findById(req.userId)
            user?.let { toResponseDTO(req, it) }
        }
    }


    fun listUserRequests(userId: String): List<EntryRequestResponseDTO> {
        val requests = entryRequestRepository.findByUserId(userId)

        val user = userRepository.findById(userId)
            ?: throw IllegalStateException("Usuário não encontrado")

        return requests.map { req ->
            toResponseDTO(req, user)
        }
    }

    fun approveRequest(requestId: String) {
        val request = entryRequestRepository.findById(requestId)
            ?: throw IllegalArgumentException("Solicitação não encontrada")

        if (request.status != RequestStatus.PENDING) {
            throw IllegalStateException("Esta solicitação já foi processada.")
        }

        val newMember = OrganizationMember(
            organizationId = request.organizationId,
            userId = request.userId,
            role = request.role,
            status = MemberStatus.ACTIVE,
            faceImageId = null
        )
        organizationMemberRepository.save(newMember)

        when (newMember.role) {
            Role.MEMBER -> organizationRepository.addMemberToOrganization(newMember.organizationId, newMember.userId)
            Role.VALIDATOR -> organizationRepository.addValidatorToOrganization(newMember.organizationId, newMember.userId)
            Role.ADMIN -> organizationRepository.addAdminToOrganization(newMember.organizationId, newMember.userId)
        }

        entryRequestRepository.updateStatus(request.id, RequestStatus.APPROVED)

        syncService.syncNewMember(newMember.organizationId, newMember.userId)
    }

    fun getRequestById(id: String): EntryRequestResponseDTO? {
        val request = entryRequestRepository.findById(id) ?: return null
        val user = userRepository.findById(request.userId) ?: return null
        return toResponseDTO(request, user)
    }


    fun updateRequest(id: String, role: Role?): EntryRequestResponseDTO {
        val request = entryRequestRepository.findById(id)
            ?: throw IllegalArgumentException("Solicitação não encontrada")

        if (request.status != RequestStatus.PENDING) {
            throw IllegalStateException("Apenas solicitações pendentes podem ser editadas.")
        }

        // se o role mudou atualiza, se nao, mantem o antigo
        val updatedRequest = if (role != null) request.copy(role = role) else request

        val savedRequest = entryRequestRepository.save(updatedRequest)

        val user = userRepository.findById(savedRequest.userId)
            ?: throw IllegalStateException("Usuário não encontrado")

        return toResponseDTO(savedRequest, user)
    }

    fun deleteRequest(id: String) {
        val request = entryRequestRepository.findById(id)
            ?: throw IllegalArgumentException("Solicitação não encontrada")


        entryRequestRepository.delete(id)
    }

    fun rejectRequest(requestId: String) {
        val request = entryRequestRepository.findById(requestId)
            ?: throw IllegalArgumentException("Solicitação não encontrada")

        if (request.status != RequestStatus.PENDING) {
            throw IllegalStateException("Solicitação não está pendente.")
        }

        entryRequestRepository.updateStatus(requestId, RequestStatus.DENIED)
    }


    private fun toResponseDTO(entryRequest: EntryRequest, user: User): EntryRequestResponseDTO {
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