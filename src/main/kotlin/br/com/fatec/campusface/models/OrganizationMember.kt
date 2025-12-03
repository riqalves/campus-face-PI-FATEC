package br.com.fatec.campusface.models

import java.time.Instant


data class OrganizationMember(
    val id: String = "",
    val organizationId: String = "",
    val userId: String = "",
    val role: Role = Role.MEMBER,
    val status: MemberStatus = MemberStatus.ACTIVE,
    val faceImageId: String? = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class Role {
    MEMBER,
    VALIDATOR,
    ADMIN
}

enum class MemberStatus {
    PENDING,
    ACTIVE,
    INACTIVE,
}