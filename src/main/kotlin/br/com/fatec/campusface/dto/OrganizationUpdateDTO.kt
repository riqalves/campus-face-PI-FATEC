package br.com.fatec.campusface.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class OrganizationUpdateDTO(
    @field:NotBlank(message = "O nome é obrigatório")
    @field:Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    val name: String,

    @field:NotBlank(message = "A descrição é obrigatória")
    @field:Size(max = 255, message = "A descrição não pode exceder 255 caracteres")
    val description: String
)