package br.com.fatec.campusface.models

import java.time.Instant

data class AuthCode(
    val id: String = "",
    val code: String = "",
    val userId: String = "",
    val organizationId: String = "",
    val expirationTime: Instant = Instant.now(),
    val valid: Boolean = true
)