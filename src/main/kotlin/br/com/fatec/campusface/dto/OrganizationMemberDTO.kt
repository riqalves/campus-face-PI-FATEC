package br.com.fatec.campusface.dto

import br.com.fatec.campusface.models.MemberStatus
import br.com.fatec.campusface.models.Role
import java.time.Instant

data class OrganizationMemberDTO(
    val id: String,
    val role: Role,
    val status: MemberStatus,
    val joinedAt: Instant,
    val user: UserDTO
)

data class MemberUpdateDTO(
    val role: Role?,
    val status: MemberStatus?,
)