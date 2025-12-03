package br.com.fatec.campusface.dto

import jakarta.validation.constraints.NotBlank

data class RegisteredClientResponseDTO(
    val id: String,
    val hubCode: String,
    val machineId: String, // Novo campo na resposta
    val ipAddress: String,
    val status: String
)

data class ClientCheckinDTO(
    @field:NotBlank(message = "O código do Hub (hubCode) é obrigatório")
    val hubCode: String, // Renomeado de hub_id para hubCode

    @field:NotBlank(message = "O ID da máquina é obrigatório")
    val machineId: String, // Novo Identificador Único do Hardware

    @field:NotBlank(message = "O endereço do servidor é obrigatório (IP:PORTA)")
    val server: String
)