package br.com.fatec.campusface.models

import java.time.Instant


data class Organization(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val hubCode: String = "",
    val adminIds: List<String> = emptyList(),
    val validatorIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)