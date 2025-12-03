package br.com.fatec.campusface.service

import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.dto.UserUpdateDTO
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val cloudinaryService: CloudinaryService,
    private val imageProcessingService: ImageProcessingService,
) {

    @Value("\${default.organization.id}")
    private lateinit var defaultOrganizationId: String

    fun createUser(userData: User, imageFile: MultipartFile?): UserDTO {
        // validacao de email
        if (userRepository.findByEmail(userData.email) != null) {
            throw IllegalArgumentException("Email já cadastrado.")
        }
        // Upload da imagem (se ouver)
        // o User generico pode ter foto, mas sera usada quando virar member em alguma org
        var imagePublicId: String? = null
        if (imageFile != null) {
            val processedImageBytes = imageProcessingService.processImageForApi(imageFile)
            val uploadResult = cloudinaryService.upload(processedImageBytes)
            imagePublicId = uploadResult["public_id"]
                ?: throw IllegalStateException("Public ID não foi retornado pelo Cloudinary.")
        }

        val userToSave = userData.copy(
            hashedPassword = passwordEncoder.encode(userData.hashedPassword),
            faceImageId = imagePublicId
        )
        val savedUser = userRepository.save(userToSave)

        return savedUser.toDTO()
    }

    fun updateProfileImage(userId:String,  imageFile: MultipartFile):UserDTO {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuario nao encontrado")
        validateImage(imageFile)

        //Se tiver foto antiga deleta do cloudinary
        user.faceImageId?.let { cloudinaryService.delete(it) }

        val processedImageBytes = imageProcessingService.processImageForApi(imageFile)
        val uploadResult = cloudinaryService.upload(processedImageBytes)
        val newPublicId = uploadResult["public_id"]
            ?:throw IllegalStateException("Erro no upload da imagem")

        val updatedUser = user.copy(faceImageId = newPublicId)
        userRepository.save(updatedUser)
        return updatedUser.toDTO()
    }

    /**
     * Atualiza os dados cadastrais do usuário (Nome, Email, Senha, Documento).
     */
    fun updateUser(userId: String, data:  UserUpdateDTO): UserDTO {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuário não encontrado.")

        println("DEBUG USERSERVICE - UPDATEUSER: $userId, $data")
        // validação de Email Único (se estiver trocando)
        if (!data.email.isNullOrBlank() && data.email != user.email) {
            val emailOwner = userRepository.findByEmail(data.email)
            if (emailOwner != null && emailOwner.id != userId) {
                throw IllegalArgumentException("Este e-mail já está em uso por outro usuário.")
            }
        }


        // atualização dos campos Mantém o antigo se o novo for nulo
        val updatedUser = user.copy(
            fullName = if (!data.fullName.isNullOrBlank()) data.fullName else user.fullName,
            email = if (!data.email.isNullOrBlank()) data.email else user.email,
            document = if (!data.document.isNullOrBlank()) data.document else user.document,

            // Só criptografa se a senha foi enviada
            hashedPassword = if (!data.password.isNullOrBlank()) {
                passwordEncoder.encode(data.password)
            } else {
                user.hashedPassword
            },

            updatedAt = java.time.Instant.now()
        )

        userRepository.save(updatedUser)

        return updatedUser.toDTO()
    }



    fun listUsers(): List<UserDTO> =
        userRepository.findAll().map { user -> user.toDTO() }

    fun getUserByEmail(email: String): UserDTO? {
        return userRepository.findByEmail(email)?.toDTO()
    }

    fun getUserById(id: String): UserDTO? {
        return userRepository.findById(id)?.toDTO()
    }

    fun deleteUser(id: String): Boolean {
        val user = userRepository.findById(id)
        // Se tiver foto, deleta do Cloudinary
        user?.faceImageId?.let { cloudinaryService.delete(it) }

        return userRepository.delete(id)
    }

    fun validateEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        return emailRegex.matches(email)
    }


    private fun User.toDTO(): UserDTO {
        // Gera URL temporária apenas se tiver imagem
        val temporaryImageUrl = this.faceImageId?.let { publicId ->
            cloudinaryService.generateSignedUrl(publicId)
        }
        return UserDTO.fromEntity(this, temporaryImageUrl)
    }

    private fun validateImage(imageFile: MultipartFile) {
        // Validação de tipo de arquivo
        val allowedTypes = listOf("image/png", "image/jpeg", "image/jpg")
        if (imageFile.contentType !in allowedTypes) {
            throw IllegalArgumentException("Formato de imagem inválido. Apenas PNG, JPG e JPEG são permitidos.")
        }

        // Validação de tamanho
        val maxSizeInBytes = 10 * 1024 * 1024 // 10 MB
        if (imageFile.size > maxSizeInBytes) {
            throw IllegalArgumentException("A imagem excede o tamanho máximo de 5MB.")
        }
    }

}
