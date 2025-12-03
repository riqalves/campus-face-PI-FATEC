package br.com.fatec.campusface.service

import br.com.fatec.campusface.dto.ChangeRequestResponseDTO
import br.com.fatec.campusface.models.ChangeRequest
import br.com.fatec.campusface.models.RequestStatus
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.repository.ChangeRequestRepository
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import br.com.fatec.campusface.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

@Service
class ChangeRequestService(
    private val changeRequestRepository: ChangeRequestRepository,
    private val memberRepository: OrganizationMemberRepository,
    private val userRepository: UserRepository,
    private val cloudinaryService: CloudinaryService,
    private val imageProcessingService: ImageProcessingService,
    private val syncService: SyncService
) {

    fun createRequest(userId: String, organizationId: String, image: MultipartFile): ChangeRequest {
        println("DEBUG [Service] - createRequest iniciado para User: $userId na Org: $organizationId")

        val member = memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
            ?: throw IllegalArgumentException("Você não é membro desta organização.")

        val pending = changeRequestRepository.findPendingByOrganizationId(organizationId)
        if (pending.any { it.userId == userId }) {
            println("DEBUG [Service] - Erro: Usuário já tem pendência.")
            throw IllegalStateException("Você já possui uma solicitação de troca pendente.")
        }

        println("DEBUG [Service] - Processando imagem...")
        val processedBytes = imageProcessingService.processImageForApi(image)
        val uploadResult = cloudinaryService.upload(processedBytes)
        val publicId = uploadResult["public_id"]
            ?: throw IllegalStateException("Erro ao fazer upload da imagem.")

        println("DEBUG [Service] - Imagem enviada ao Cloudinary. PublicID: $publicId")

        val request = ChangeRequest(
            userId = userId,
            organizationId = organizationId,
            newFaceImageId = publicId,
            status = RequestStatus.PENDING,
            requestedAt = Instant.now()
        )

        return changeRequestRepository.save(request)
    }

    fun listPendingRequests(organizationId: String): List<ChangeRequestResponseDTO> {
        val requests = changeRequestRepository.findPendingByOrganizationId(organizationId)

        return requests.mapNotNull { req ->
            val user = userRepository.findById(req.userId)
            val member = memberRepository.findByUserIdAndOrganizationId(req.userId, organizationId)

            if (user != null && member != null) {
                val currentFaceId = member.faceImageId?.ifBlank { null } ?: user.faceImageId
                val currentUrl = currentFaceId?.let { cloudinaryService.generateSignedUrl(it) }
                val newUrl = cloudinaryService.generateSignedUrl(req.newFaceImageId)

                ChangeRequestResponseDTO(
                    id = req.id,
                    status = req.status,
                    requestedAt = req.requestedAt,
                    organizationId = req.organizationId,
                    userFullName = user.fullName,
                    currentFaceUrl = currentUrl,
                    newFaceUrl = newUrl
                )
            } else {
                null
            }
        }
    }

    fun reviewRequest(requestId: String, adminUserId: String, approved: Boolean) {
        println("DEBUG [Service] - reviewRequest: RequestID=$requestId, Admin=$adminUserId, Aprovado=$approved")

        val request = changeRequestRepository.findById(requestId)
            ?: throw IllegalArgumentException("Solicitação não encontrada.")

        if (request.status != RequestStatus.PENDING) {
            println("DEBUG [Service] - Erro: Status atual é ${request.status}")
            throw IllegalStateException("Solicitação já processada.")
        }

        val adminMember = memberRepository.findByUserIdAndOrganizationId(adminUserId, request.organizationId)
        if (adminMember == null || adminMember.role != Role.ADMIN) {
            println("DEBUG [Service] - Erro: Usuário $adminUserId não é ADMIN.")
            throw IllegalAccessException("Apenas ADMINs podem revisar solicitações.")
        }

        if (approved) {
            approve(request)
        } else {
            reject(request)
        }
    }

    private fun approve(request: ChangeRequest) {
        val member = memberRepository.findByUserIdAndOrganizationId(request.userId, request.organizationId)
            ?: throw IllegalStateException("Membro não encontrado.")

        // Atualiza o OrganizationMember com a NOVA foto
        memberRepository.updateFaceImageId(member.id, request.newFaceImageId)

        //  atualiza Status do Pedido
        val updatedReq = request.copy(status = RequestStatus.APPROVED, updatedAt = Instant.now())
        changeRequestRepository.save(updatedReq)

        syncService.syncNewMember(request.organizationId, request.userId)
    }

    private fun reject(request: ChangeRequest) {
        println("DEBUG [Service] - REJEITANDO solicitação ${request.id}...")

        cloudinaryService.delete(request.newFaceImageId)
        println("DEBUG [Service] - Imagem ${request.newFaceImageId} removida do Cloudinary.")

        val updatedReq = request.copy(status = RequestStatus.DENIED, updatedAt = Instant.now())
        changeRequestRepository.save(updatedReq)
    }
}