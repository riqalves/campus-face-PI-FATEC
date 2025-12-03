package br.com.fatec.campusface.service

import br.com.fatec.campusface.dto.OrganizationCreateDTO
import br.com.fatec.campusface.dto.OrganizationResponseDTO
import br.com.fatec.campusface.dto.OrganizationUpdateDTO
import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.models.*
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import br.com.fatec.campusface.repository.OrganizationRepository
import br.com.fatec.campusface.repository.UserRepository
import org.springframework.stereotype.Service


@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val userRepository: UserRepository,
    private val cloudinaryService: CloudinaryService,
) {


    fun createOrganization(orgData: OrganizationCreateDTO, creatorAdmin: User): Organization {
        if (organizationRepository.findByHubCode(orgData.hubCode) != null) {
            throw IllegalArgumentException("Organizacao já existe, codigo ja em uso")
        }

        val newOrganization = Organization(
            name = orgData.name,
            description = orgData.description,
            hubCode = orgData.hubCode,
            adminIds = listOf(creatorAdmin.id),
        )

        val savedOrg = organizationRepository.save(newOrganization)

        val adminMember = OrganizationMember(
            organizationId = savedOrg.id,
            userId = creatorAdmin.id,
            role = Role.ADMIN,
            status = MemberStatus.ACTIVE,
            faceImageId = creatorAdmin.faceImageId, // O admin pode ter foto ou nao
        )

        organizationMemberRepository.save(adminMember)
        return savedOrg
    }


    fun getAllOrganizations(): List<OrganizationResponseDTO> {
        val organizations = organizationRepository.findAll()
        return organizations.map {org -> hydrateOrganization(org)}
    }

    fun getOrganizationById(id: String): OrganizationResponseDTO? {
        val organization = organizationRepository.findById(id)
        return organization?.let { org -> hydrateOrganization(org) }
    }

    fun getOrganizationByHubCode(hubCode: String): OrganizationResponseDTO? {
        val organization = organizationRepository.findByHubCode(hubCode)
        return organization?.let { org -> hydrateOrganization(org) }
    }

    fun updateOrganization(id: String, orgData: OrganizationUpdateDTO): OrganizationResponseDTO? {
        val existingOrg = organizationRepository.findById(id) ?: return null

        val updatedOrg = existingOrg.copy(
            name = orgData.name,
            description = orgData.description
        )

        val savedOrg = organizationRepository.update(id, updatedOrg)

        return savedOrg?.let { org -> hydrateOrganization(org) }
    }

    fun deleteOrganization(id: String): Boolean {
        return organizationRepository.delete(id)
    }



    /**
     * Lista todas as organizações onde o usuário é membro (ADMIN, VALIDATOR ou MEMBER).
     */
    fun listUserHubs(userId: String): List<OrganizationResponseDTO> {
        // procura os vinculos od user
        val memberships = organizationMemberRepository.findAllByUserId(userId)

        if (memberships.isEmpty()) {
            return emptyList()
        }

        // pega os ids das organizações
        val organizationIds = memberships.map { it.organizationId }.distinct()

        // detalhes das organizações
        val organizations = organizationRepository.findAllByIds(organizationIds)

        return organizations.map { org -> hydrateOrganization(org) }
    }


    /**
     * Converte Organization (Entity) -> OrganizationResponseDTO.
     * Busca os dados completos dos usuários para preencher as listas.
     */
    private fun hydrateOrganization(organization: Organization): OrganizationResponseDTO {
        val admins = userRepository.findAllByIds(organization.adminIds).map { it.toDTO() }
        val validators = userRepository.findAllByIds(organization.validatorIds).map { it.toDTO() }
        val members = userRepository.findAllByIds(organization.memberIds).map { it.toDTO() }

        return OrganizationResponseDTO(
            id = organization.id,
            name = organization.name,
            description = organization.description,
            hubCode = organization.hubCode,
            admins = admins,
            validators = validators,
            members = members
        )
    }


    /**
     * Converte User (Entity) -> UserDTO.
     * Gera URL assinada temporária para a imagem.
     */
    private fun User.toDTO(): UserDTO {
        val temporaryImageUrl = this.faceImageId?.let { publicId ->
            cloudinaryService.generateSignedUrl(publicId)
        }
        return UserDTO.fromEntity(this, temporaryImageUrl)
    }

}