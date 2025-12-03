package br.com.fatec.campusface.models

import java.time.Instant

data class EntryRequest(
    val id: String = "",

    val userId: String = "",
    val organizationId: String = "",

    val hubCode: String = "",

    val role: Role = Role.MEMBER,
    val status: RequestStatus = RequestStatus.PENDING,

    val requestedAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
