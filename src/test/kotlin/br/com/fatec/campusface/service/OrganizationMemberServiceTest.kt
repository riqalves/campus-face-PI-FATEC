package br.com.fatec.campusface.service

import br.com.fatec.campusface.models.MemberStatus
import br.com.fatec.campusface.models.OrganizationMember
import br.com.fatec.campusface.models.Role
import br.com.fatec.campusface.models.User
import br.com.fatec.campusface.repository.OrganizationMemberRepository
import br.com.fatec.campusface.repository.UserRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrganizationMemberServiceTest {

    @MockK
    lateinit var memberRepository: OrganizationMemberRepository

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var cloudinaryService: CloudinaryService

    @InjectMockKs
    lateinit var memberService: OrganizationMemberService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    // --- CENÁRIOS DE LEITURA (Get) ---

    @Test
    fun `getAllMembers deve retornar lista de DTOs hidratados`() {
        // ARRANGE
        val orgId = "org123"
        val userId1 = "user1"
        val userId2 = "user2"

        val member1 = OrganizationMember(id = "m1", userId = userId1, organizationId = orgId, role = Role.ADMIN)
        val member2 = OrganizationMember(id = "m2", userId = userId2, organizationId = orgId, role = Role.MEMBER)

        val user1 = User(id = userId1, fullName = "Admin User", faceImageId = "face1")
        val user2 = User(id = userId2, fullName = "Member User", faceImageId = "face2")

        // Mock do repositório de membros
        every { memberRepository.findAllByOrganizationId(orgId) } returns listOf(member1, member2)

        // Mock do repositório de usuários
        every { userRepository.findAllByIds(listOf(userId1, userId2)) } returns listOf(user1, user2)

        // Mock do Cloudinary (para gerar URLs assinadas)
        every { cloudinaryService.generateSignedUrl(any()) } returns "https://signed.url"

        // ACT
        val result = memberService.getAllMembers(orgId)

        // ASSERT
        assertEquals(2, result.size)
        assertEquals("Admin User", result[0].user.fullName)
        assertEquals("Member User", result[1].user.fullName)

        // Verifica se chamou a otimização de buscar usuários em lote
        verify(exactly = 1) { userRepository.findAllByIds(any()) }
    }

    @Test
    fun `getMemberById deve retornar DTO se encontrado`() {
        // ARRANGE
        val memberId = "m1"
        val userId = "u1"
        val member = OrganizationMember(id = memberId, userId = userId, organizationId = "org1")
        val user = User(id = userId, fullName = "Teste")

        every { memberRepository.findById(memberId) } returns member
        every { userRepository.findById(userId) } returns user
        every { cloudinaryService.generateSignedUrl(any()) } returns "url"

        // ACT
        val result = memberService.getMemberById(memberId)

        // ASSERT
        assertNotNull(result)
        assertEquals(memberId, result?.id)
        assertEquals(userId, result?.user?.id)
    }

    @Test
    fun `getMemberById deve retornar null se membro nao existir`() {
        // ARRANGE
        every { memberRepository.findById("inexistente") } returns null

        // ACT
        val result = memberService.getMemberById("inexistente")

        // ASSERT
        assertNull(result)
    }

    // --- CENÁRIOS DE ESCRITA (Update/Delete) ---

    @Test
    fun `updateMemberRole deve atualizar cargo e retornar DTO atualizado`() {
        // ARRANGE
        val memberId = "m1"
        val userId = "u1"
        val oldMember = OrganizationMember(id = memberId, userId = userId, role = Role.MEMBER)
        val user = User(id = userId)

        // Busca inicial (encontra o membro)
        every { memberRepository.findById(memberId) } returns oldMember

        // Ação de Update (void)
        every { memberRepository.updateRole(memberId, Role.ADMIN) } just Runs

        // Busca do usuário para montar o retorno
        every { userRepository.findById(userId) } returns user
        every { cloudinaryService.generateSignedUrl(any()) } returns "url"

        // ACT
        val result = memberService.updateMemberRole(memberId, Role.ADMIN)

        // ASSERT
        assertNotNull(result)

        verify(exactly = 1) { memberRepository.updateRole(memberId, Role.ADMIN) }
    }

    @Test
    fun `updateMemberRole deve lancar erro se membro nao existir`() {
        // ARRANGE
        every { memberRepository.findById("fantasma") } returns null

        // ACT & ASSERT
        assertThrows<IllegalArgumentException> {
            memberService.updateMemberRole("fantasma", Role.ADMIN)
        }
    }

    @Test
    fun `removeMember deve chamar delete no repositorio`() {
        // ARRANGE
        val member = OrganizationMember(id = "m1", userId = "u1")
        every { memberRepository.findById("m1") } returns member
        every { memberRepository.delete("m1") } just Runs

        // ACT
        memberService.removeMember("m1")

        // ASSERT
        verify(exactly = 1) { memberRepository.delete("m1") }
    }

    @Test
    fun `updateMemberStatus deve atualizar status e retornar DTO`() {
        // ARRANGE
        val memberId = "m1"
        val userId = "u1"
        val initialMember = OrganizationMember(id = memberId, userId = userId, status = MemberStatus.PENDING)
        val user = User(id = userId)

        // Simula encontrar o membro no banco
        every { memberRepository.findById(memberId) } returns initialMember

        // Simula a ação de update (void)
        every { memberRepository.updateStatus(memberId, MemberStatus.ACTIVE) } just Runs

        // Simula as dependências do 'getMemberById' (chamado no return)
        every { userRepository.findById(userId) } returns user
        every { cloudinaryService.generateSignedUrl(any()) } returns "url"

        // ACT
        val result = memberService.updateMemberStatus(memberId, MemberStatus.ACTIVE)

        // ASSERT
        assertNotNull(result)
        // verifica se o método de update foi chamado no repositório
        verify(exactly = 1) { memberRepository.updateStatus(memberId, MemberStatus.ACTIVE) }
    }

    @Test
    fun `updateMemberStatus nao deve chamar repositorio se o status for igual ao atual`() {
        // ARRANGE
        val memberId = "m1"
        val userId = "u1"
        // Membro JÁ ESTÁ como ACTIVE
        val member = OrganizationMember(id = memberId, userId = userId, status = MemberStatus.ACTIVE)
        val user = User(id = userId)

        every { memberRepository.findById(memberId) } returns member

        // Dependências para o retorno (getMemberById)
        every { userRepository.findById(userId) } returns user
        every { cloudinaryService.generateSignedUrl(any()) } returns "url"

        // ACT
        // Tentamos mudar para ACTIVE (o mesmo que já estava)
        memberService.updateMemberStatus(memberId, MemberStatus.ACTIVE)

        // ASSERT
        // Garante que o updateStatus JAMAIS foi chamado (Economia de banco)
        verify(exactly = 0) { memberRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `updateMemberStatus deve lancar erro se membro nao for encontrado`() {
        // ARRANGE
        val invalidId = "ghost_id"
        every { memberRepository.findById(invalidId) } returns null

        // ACT & ASSERT
        val exception = assertThrows<IllegalArgumentException> {
            memberService.updateMemberStatus(invalidId, MemberStatus.ACTIVE)
        }

        assertEquals("Membro não encontrado", exception.message)
    }
}