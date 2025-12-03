package br.com.fatec.campusface.controller

import br.com.fatec.campusface.configuration.SecurityFilter
import br.com.fatec.campusface.dto.ApiResponse
import br.com.fatec.campusface.dto.MemberUpdateDTO
import br.com.fatec.campusface.dto.OrganizationMemberDTO
import br.com.fatec.campusface.dto.UserDTO
import br.com.fatec.campusface.models.MemberStatus
import br.com.fatec.campusface.models.OrganizationMember
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import br.com.fatec.campusface.repository.UserRepository
import br.com.fatec.campusface.service.AuthService
import br.com.fatec.campusface.service.OrganizationMemberService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(OrganizationMemberController::class)
@AutoConfigureMockMvc(addFilters = false) // Desativa filtros de segurança para focar na lógica do controller
class OrganizationMemberControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // Mocks do Controller
    @MockkBean
    lateinit var memberService: OrganizationMemberService

    @MockkBean
    lateinit var memberRepository: OrganizationMemberRepository

    // Mocks de Infraestrutura (necessários para o contexto subir)
    @MockkBean
    lateinit var securityFilter: SecurityFilter
    @MockkBean
    lateinit var userRepository: UserRepository
    @MockkBean
    lateinit var authService: AuthService

    // --- DADOS AUXILIARES ---
    private val orgId = "org123"
    private val adminUserId = "adminUser"
    private val memberUserId = "normalUser"
    private val memberId = "mem_001"

    // Usuário Autenticado (Admin)
    private val adminAuth = UsernamePasswordAuthenticationToken(
        User(id = adminUserId, email = "admin@test.com"), null, emptyList()
    )

    // Usuário Autenticado (Membro Comum)
    private val memberAuth = UsernamePasswordAuthenticationToken(
        User(id = memberUserId, email = "user@test.com"), null, emptyList()
    )

    // --- TESTES: LIST MEMBERS ---

    @Test
    fun `listMembers deve retornar 200 OK quando usuario for ADMIN`() {
        // ARRANGE
        // Mock da permissão: O usuário logado (adminUser) é ADMIN da org123
        val adminMembership = OrganizationMember(userId = adminUserId, organizationId = orgId, role = Role.ADMIN, status = MemberStatus.ACTIVE)
        every { memberRepository.findByUserIdAndOrganizationId(adminUserId, orgId) } returns adminMembership

        // Mock do serviço
        val listDTO = listOf(OrganizationMemberDTO(id = "m1", role = Role.MEMBER, status = MemberStatus.ACTIVE, joinedAt = Instant.now(), user = UserDTO(id="u1", fullName="User", email="e", document="d", faceImageId="f", createdAt=Instant.now(), updatedAt=Instant.now())))
        every { memberService.getAllMembers(orgId, null) } returns listDTO

        // ACT & ASSERT
        mockMvc.perform(get("/members/organization/$orgId")
            .principal(adminAuth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    fun `listMembers deve retornar 403 Forbidden quando usuario nao tiver permissao`() {
        // ARRANGE
        // Mock da permissão: Retorna null (não é membro) ou membro comum
        val commonMembership = OrganizationMember(userId = memberUserId, organizationId = orgId, role = Role.MEMBER, status = MemberStatus.ACTIVE)
        every { memberRepository.findByUserIdAndOrganizationId(memberUserId, orgId) } returns commonMembership

        // ACT & ASSERT
        mockMvc.perform(get("/members/organization/$orgId")
            .principal(memberAuth))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Você não tem permissão para visualizar membros desta organização."))
    }

    // --- TESTES: UPDATE MEMBER ---

    @Test
    fun `updateMember deve retornar 200 OK ao atualizar cargo`() {
        // ARRANGE
        val updateDto = MemberUpdateDTO(role = Role.VALIDATOR, status = null)

        // 1. Busca o membro alvo (para saber a org)
        val targetMember = OrganizationMember(id = memberId, userId = "otherUser", organizationId = orgId)
        every { memberRepository.findById(memberId) } returns targetMember

        // 2. Verifica permissão do Admin
        val adminMembership = OrganizationMember(userId = adminUserId, organizationId = orgId, role = Role.ADMIN, status = MemberStatus.ACTIVE)
        every { memberRepository.findByUserIdAndOrganizationId(adminUserId, orgId) } returns adminMembership

        // 3. Executa o update no serviço
        val responseDto = OrganizationMemberDTO(id = memberId, role = Role.VALIDATOR, status = MemberStatus.ACTIVE, joinedAt = Instant.now(), user = UserDTO(id="u2", fullName="Other", email="e", document="d", faceImageId="f", createdAt=Instant.now(), updatedAt=Instant.now()))
        every { memberService.updateMemberRole(memberId, Role.VALIDATOR) } returns responseDto

        // ACT & ASSERT
        mockMvc.perform(put("/members/$memberId")
            .principal(adminAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.role").value("VALIDATOR"))
    }

    @Test
    fun `updateMember deve retornar 400 Bad Request se Admin tentar se rebaixar`() {
        // ARRANGE
        // Tentativa de mudar o próprio cargo para MEMBER
        val updateDto = MemberUpdateDTO(role = Role.MEMBER, status = null)

        // O membro alvo é o PRÓPRIO admin logado
        val targetMember = OrganizationMember(id = memberId, userId = adminUserId, organizationId = orgId)
        every { memberRepository.findById(memberId) } returns targetMember

        // Permissão ok
        val adminMembership = OrganizationMember(userId = adminUserId, organizationId = orgId, role = Role.ADMIN, status = MemberStatus.ACTIVE)
        every { memberRepository.findByUserIdAndOrganizationId(adminUserId, orgId) } returns adminMembership

        // ACT & ASSERT
        mockMvc.perform(put("/members/$memberId")
            .principal(adminAuth) // Admin logado
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Você não pode rebaixar seu próprio cargo de ADMIN."))
    }

    // --- TESTES: DELETE MEMBER ---

    @Test
    fun `removeMember deve retornar 200 OK quando Admin remove membro`() {
        // ARRANGE
        val targetMember = OrganizationMember(id = memberId, userId = "userToRemove", organizationId = orgId)
        every { memberRepository.findById(memberId) } returns targetMember

        // Permissão Admin
        val adminMembership = OrganizationMember(userId = adminUserId, organizationId = orgId, role = Role.ADMIN, status = MemberStatus.ACTIVE)
        every { memberRepository.findByUserIdAndOrganizationId(adminUserId, orgId) } returns adminMembership

        // Serviço remove
        every { memberService.removeMember(memberId) } just runs

        // ACT & ASSERT
        mockMvc.perform(delete("/members/$memberId")
            .principal(adminAuth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Membro removido da organização."))

        verify(exactly = 1) { memberService.removeMember(memberId) }
    }

    // --- TESTES: GET MEMBER (BUSCA ESPECÍFICA) ---

    @Test
    fun `getMember deve retornar 404 Not Found se membro nao existir`() {
        // ARRANGE
        val invalidId = "ghost_mem"
        every { memberService.getMemberById(invalidId) } returns null

        // ACT & ASSERT
        mockMvc.perform(get("/members/$invalidId")
            .principal(adminAuth))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Membro não encontrado."))
    }

    @Test
    fun `getMember deve retornar 200 OK se o usuario estiver acessando seu proprio registro (isSelf)`() {
        // ARRANGE
        val myMemberId = "my_mem_id"
        // O usuário logado é 'normalUser' (id: normalUser) definido no setup da classe
        val myAuth = memberAuth

        // DTO retornado pelo serviço: O ID do usuário dentro do DTO bate com o ID do token
        val myUserDto = UserDTO(id = memberUserId, fullName = "Me", email = "me@me.com", document = "1", faceImageId = "f", createdAt = Instant.now(), updatedAt = Instant.now())
        val myMemberDto = OrganizationMemberDTO(id = myMemberId, role = Role.MEMBER, status = MemberStatus.ACTIVE, joinedAt = Instant.now(), user = myUserDto)

        every { memberService.getMemberById(myMemberId) } returns myMemberDto

        // ACT & ASSERT
        mockMvc.perform(get("/members/$myMemberId")
            .principal(myAuth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(myMemberId))
        // Nota: Não precisamos mockar o Repository aqui porque o 'isSelf' dá true e o if de segurança é pulado
    }

    @Test
    fun `getMember deve retornar 200 OK se um ADMIN acessar dados de outro usuario`() {
        // ARRANGE
        val targetMemberId = "other_mem_id"
        // Usuário logado é Admin (id: adminUser)

        // DTO do alvo (é outro usuário)
        val targetUserDto = UserDTO(id = "otherUser", fullName = "Other", email = "o@o.com", document = "2", faceImageId = "f", createdAt = Instant.now(), updatedAt = Instant.now())
        val targetMemberDto = OrganizationMemberDTO(id = targetMemberId, role = Role.MEMBER, status = MemberStatus.ACTIVE, joinedAt = Instant.now(), user = targetUserDto)

        // serviço retorna o DTO
        every { memberService.getMemberById(targetMemberId) } returns targetMemberDto

        // Mock dos métodos auxiliares privados do Controller:
        // 'authenticationOrgId' chama findById
        val targetMemberEntity = OrganizationMember(id = targetMemberId, userId = "otherUser", organizationId = orgId)
        every { memberRepository.findById(targetMemberId) } returns targetMemberEntity

        // 'hasPermission' chama findByUserIdAndOrganizationId para o Admin
        val adminMembership = OrganizationMember(userId = adminUserId, organizationId = orgId, role = Role.ADMIN, status = MemberStatus.ACTIVE)
        every { memberRepository.findByUserIdAndOrganizationId(adminUserId, orgId) } returns adminMembership

        // ACT & ASSERT
        mockMvc.perform(get("/members/$targetMemberId")
            .principal(adminAuth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(targetMemberId))
    }

    @Test
    fun `getMember deve retornar 403 Forbidden se usuario comum tentar ver dados de outro`() {
        // ARRANGE
        val targetMemberId = "admin_mem_id"
        // Usuário logado é Comum (id: normalUser) tentando ver dados do Admin

        val targetUserDto = UserDTO(id = adminUserId, fullName = "Boss", email = "b@b.com", document = "3", faceImageId = "f", createdAt = Instant.now(), updatedAt = Instant.now())
        val targetMemberDto = OrganizationMemberDTO(id = targetMemberId, role = Role.ADMIN, status = MemberStatus.ACTIVE, joinedAt = Instant.now(), user = targetUserDto)

        every { memberService.getMemberById(targetMemberId) } returns targetMemberDto

        // Mocks de Segurança:
        // Busca a entidade para saber a Org
        val targetEntity = OrganizationMember(id = targetMemberId, userId = adminUserId, organizationId = orgId)
        every { memberRepository.findById(targetMemberId) } returns targetEntity

        // Busca a permissão do usuário logado (MEMBER comum)
        val myMembership = OrganizationMember(userId = memberUserId, organizationId = orgId, role = Role.MEMBER, status = MemberStatus.ACTIVE)
        every { memberRepository.findByUserIdAndOrganizationId(memberUserId, orgId) } returns myMembership

        // ACT & ASSERT
        mockMvc.perform(get("/members/$targetMemberId")
            .principal(memberAuth))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Acesso negado."))
    }
}