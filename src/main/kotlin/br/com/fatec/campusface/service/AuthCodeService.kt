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
    private val orgMemberService: OrganizationMemberService // Reutilizamos para hidratar o DTO
) {

    /**
     * Gera um QR Code para um Membro entrar em uma Organização.
     */
    fun generateCode(userId: String, organizationId: String): GeneratedCodeResponse {
        val member = orgMemberRepository.findByUserIdAndOrganizationId(userId, organizationId)
            ?: throw IllegalArgumentException("Você não é membro desta organização.")

        if (member.status != MemberStatus.ACTIVE) {
            throw IllegalStateException("Seu cadastro nesta organização não está ativo (Status: ${member.status}).")
        }

        // invalida códigos velhos
        authCodeRepository.invalidatePreviousCodes(userId, organizationId)

        // gera novo código
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

    /**
     * Valida um código escaneado por um Validator.
     */
    fun validateCode(code: String, validatorUserId: String): ValidationResponseDTO {
        val authCode = authCodeRepository.findValidByCode(code)
            ?: return ValidationResponseDTO(false, "Código inválido, não encontrado ou já utilizado.", null)

        // Verificacao de validade temporal
        if (Instant.now().isAfter(authCode.expirationTime)) {
            authCodeRepository.invalidateCode(authCode.id)
            return ValidationResponseDTO(false, "Código expirado.", null)
        }

        // Verifica se o validator tem permissão na Org do código
        val validatorMember = orgMemberRepository.findByUserIdAndOrganizationId(validatorUserId, authCode.organizationId)

        if (validatorMember == null ||
            (validatorMember.role != Role.VALIDATOR && validatorMember.role != Role.ADMIN) ||
            validatorMember.status != MemberStatus.ACTIVE) {
            // Não invalidamos o código aqui, pois pode ter sido apenas um erro de quem escaneou (fiscal errado)
            throw IllegalAccessException("Você não tem permissão de VALIDATOR nesta organização.")
        }

        authCodeRepository.invalidateCode(authCode.id)

        // busca dados do dono do código para mostrar na tela do validator
        val targetMember = orgMemberRepository.findByUserIdAndOrganizationId(authCode.userId, authCode.organizationId)
            ?: return ValidationResponseDTO(false, "Usuário do código não encontrado na organização.", null)

        val memberDto = orgMemberService.getMemberById(targetMember.id)

        return ValidationResponseDTO(
            valid = true,
            message = "Acesso Autorizado!",
            member = memberDto
        )
    }
}