package br.com.fatec.campusface.service

import br.com.fatec.campusface.dto.GeneratedCodeResponse
import br.com.fatec.campusface.dto.ValidationResponseDTO
import br.com.fatec.campusface.models.AuthCode
import br.com.fatec.campusface.models.MemberStatus
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.repository.AuthCodeRepository
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AuthCodeService(
    private val authCodeRepository: AuthCodeRepository,
    private val orgMemberRepository: OrganizationMemberRepository,
    private val orgMemberService: OrganizationMemberService
) {


    fun generateCode(userId: String, organizationId: String): GeneratedCodeResponse {
        val member = orgMemberRepository.findByUserIdAndOrganizationId(userId, organizationId)
            ?: throw IllegalArgumentException("Você não é membro desta organização.")

        if (member.status != MemberStatus.ACTIVE) {
            throw IllegalStateException("Seu cadastro nesta organização não está ativo (Status: ${member.status}).")
        }

        authCodeRepository.invalidatePreviousCodes(userId, organizationId)

        val code = (100000..999999).random().toString() // Ex: 123456
        val expirationTime = Instant.now().plus(5, ChronoUnit.MINUTES)

        val newAuthCode = AuthCode(
            code = code,
            userId = userId,
            organizationId = organizationId,
            expirationTime = expirationTime
        )

        authCodeRepository.save(newAuthCode)

        return GeneratedCodeResponse(newAuthCode.code, newAuthCode.expirationTime)
    }


    fun validateCode(code: String, validatorUserId: String): ValidationResponseDTO {
        val authCode = authCodeRepository.findValidByCode(code)
            ?: return ValidationResponseDTO(false, "Código inválido, não encontrado ou já utilizado.", null)

        if (Instant.now().isAfter(authCode.expirationTime)) {
            authCodeRepository.invalidateCode(authCode.id)
            return ValidationResponseDTO(false, "Código expirado.", null)
        }

        val validatorMember = orgMemberRepository.findByUserIdAndOrganizationId(validatorUserId, authCode.organizationId)

        if (validatorMember == null ||
            (validatorMember.role != Role.VALIDATOR && validatorMember.role != Role.ADMIN) ||
            validatorMember.status != MemberStatus.ACTIVE) {
            throw IllegalAccessException("Você não tem permissão de VALIDATOR nesta organização.")
        }

        authCodeRepository.invalidateCode(authCode.id)

        val targetMember = orgMemberRepository.findByUserIdAndOrganizationId(authCode.userId, authCode.organizationId)
            ?: return ValidationResponseDTO(false, "Usuário do código não encontrado na organização.", null)

        val memberDto = orgMemberService.getMemberById(targetMember.id)

        return ValidationResponseDTO(
            valid = true,
            message = "Acesso Autorizado!",
            member = memberDto
        )
    }

    fun listAllCodes(): List<br.com.fatec.campusface.dto.AuthCodeResponseDTO> {
        return authCodeRepository.findAll().map { toResponseDTO(it) }
    }

    fun getCodeById(id: String): br.com.fatec.campusface.dto.AuthCodeResponseDTO? {
        val code = authCodeRepository.findById(id) ?: return null
        return toResponseDTO(code)
    }


    fun updateCode(id: String, dto: br.com.fatec.campusface.dto.AuthCodeUpdateDTO): br.com.fatec.campusface.dto.AuthCodeResponseDTO {
        val code = authCodeRepository.findById(id)
            ?: throw IllegalArgumentException("Código não encontrado")

        val updatedCode = code.copy(
            valid = dto.valid ?: code.valid,
            expirationTime = dto.expirationTime ?: code.expirationTime
        )

        authCodeRepository.save(updatedCode)
        return toResponseDTO(updatedCode)
    }

    fun deleteCode(id: String) {
        if (authCodeRepository.findById(id) == null) {
            throw IllegalArgumentException("Código não encontrado")
        }
        authCodeRepository.delete(id)
    }


    fun invalidateCodeManual(id: String) {
        if (authCodeRepository.findById(id) == null) {
            throw IllegalArgumentException("Código não encontrado")
        }
        authCodeRepository.invalidateCode(id)
    }

    private fun toResponseDTO(code: AuthCode) = br.com.fatec.campusface.dto.AuthCodeResponseDTO(
        id = code.id,
        code = code.code,
        userId = code.userId,
        organizationId = code.organizationId,
        expirationTime = code.expirationTime,
        valid = code.valid
    )
}