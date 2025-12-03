package br.com.fatec.campusface.dto



data class OrganizationResponseDTO(
    val id: String,
    val name: String,
    val description: String,
    val hubCode: String,
    val admins: List<UserDTO>,
    val validators: List<UserDTO>,
    val members: List<UserDTO>
)