package br.com.fatec.campusface.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class OrganizationCreateDTO(
    @field: NotBlank(message = "Nome não pode ser vazio.")
    @field:Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    val name: String,

    @field:NotBlank(message = "A descrição é obrigatória")
    @field:Size(max = 255, message = "A descrição não pode exceder 255 caracteres")
    val description: String,

    @field:NotBlank(message = "O código do Hub é obrigatório")
    val hubCode: String
)
