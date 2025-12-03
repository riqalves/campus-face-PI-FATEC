package br.com.fatec.campusface.models

import java.time.Instant

data class ChangeRequest(
    val id: String = "",

    val organizationId: String = "",

    val userId: String = "",

    val newFaceImageId: String = "",

    val status: RequestStatus = RequestStatus.PENDING,

    val requestedAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)