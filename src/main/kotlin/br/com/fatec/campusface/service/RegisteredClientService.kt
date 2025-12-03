package br.com.fatec.campusface.service

import br.com.fatec.campusface.dto.ClientCheckinDTO
import br.com.fatec.campusface.dto.RegisteredClientResponseDTO
import br.com.fatec.campusface.models.ClientStatus
import br.com.fatec.campusface.models.RegisteredClient
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import br.com.fatec.campusface.repository.OrganizationRepository
import br.com.fatec.campusface.repository.RegisteredClientRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RegisteredClientService(
    private val registeredClientRepository: RegisteredClientRepository,
    private val organizationRepository: OrganizationRepository,
    private val memberRepository: OrganizationMemberRepository,
    private val authService: AuthService
) {

    fun processCheckin(data: ClientCheckinDTO, tokenHeader: String): RegisteredClientResponseDTO {
        // 1. Resolver Token
        val tokenRaw = tokenHeader.replace("Bearer ", "").trim()
        if (tokenRaw.isBlank()) throw IllegalArgumentException("Token inválido.")

        val userId = authService.validateToken(tokenRaw)
        if (userId.isBlank()) throw IllegalArgumentException("Token expirado.")

        // 2. Validar Hub (Usando hubCode conforme solicitado)
        val organization = organizationRepository.findByHubCode(data.hubCode)
            ?: throw IllegalArgumentException("Hub não encontrado com o código: ${data.hubCode}")

        // 3. Validar Permissão
        val member = memberRepository.findByUserIdAndOrganizationId(userId, organization.id)
        if (member == null || (member.role != Role.VALIDATOR && member.role != Role.ADMIN)) {
            throw IllegalAccessException("Sem permissão de Validador.")
        }

        // 4. Limpeza do Endereço
        var cleanAddress = data.server.trim()
        if (cleanAddress.endsWith("/")) cleanAddress = cleanAddress.dropLast(1)

        cleanAddress = cleanAddress
            .replace("http://", "")
            .replace("https://", "")

        // 5. LÓGICA DE UPSERT VIA MACHINE ID
        // Verifica se essa máquina já existe no sistema
        val existingClient = registeredClientRepository.findByMachineId(data.machineId)

        val clientToSave = if (existingClient != null) {
            // Se existe, ATUALIZA o IP (caso tenha mudado) e o status
            existingClient.copy(
                organizationId = organization.id, // Pode ter mudado de hub? Se sim, atualiza.
                ipAddress = cleanAddress,         // Atualiza o IP novo
                lastCheckin = Instant.now(),
                status = ClientStatus.ONLINE,
                updatedAt = Instant.now()
            )
        } else {
            // Se não existe, CRIA um novo registro
            RegisteredClient(
                organizationId = organization.id,
                machineId = data.machineId,       // Salva o ID físico
                ipAddress = cleanAddress,
                name = "Totem Python (${organization.hubCode})",
                lastCheckin = Instant.now(),
                status = ClientStatus.ONLINE
            )
        }

        val savedClient = registeredClientRepository.save(clientToSave)

        return RegisteredClientResponseDTO(
            id = savedClient.id,
            hubCode = organization.hubCode,
            machineId = savedClient.machineId,
            ipAddress = savedClient.ipAddress,
            status = savedClient.status.name
        )
    }
}