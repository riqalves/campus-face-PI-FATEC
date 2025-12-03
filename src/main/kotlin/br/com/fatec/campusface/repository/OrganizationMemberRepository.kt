package br.com.fatec.campusface.repository

import br.com.fatec.campusface.models.MemberStatus
import br.com.fatec.campusface.models.OrganizationMember
import br.com.fatec.campusface.models.Role
import com.google.cloud.firestore.Firestore
import org.springframework.stereotype.Repository

@Repository
class OrganizationMemberRepository(private val firestore: Firestore) {

    private val collection = firestore.collection("organizationMembers")

    fun save(member: OrganizationMember): OrganizationMember {
        if (member.userId.isEmpty() || member.organizationId.isEmpty()) {
            throw IllegalArgumentException("O ID do usuário e da organização não podem ser vazios")
        }

        // Se já tiver ID, atualiza. Se não, cria um novo documento.
        val docRef = if (member.id.isNotEmpty()) collection.document(member.id) else collection.document()
        val memberWithId = member.copy(id = docRef.id)

        // Usamos .set() para criar ou sobrescrever
        docRef.set(memberWithId).get()

        return memberWithId
    }

    fun findById(id: String): OrganizationMember? {
        val document = collection.document(id).get().get()
        return if (document.exists()) {
            document.toObject(OrganizationMember::class.java)?.copy(id = document.id)
        } else {
            null
        }
    }

    /**
     * Busca o vínculo específico de um usuário com uma organização.
     * Essencial para verificar permissões (ex: "Este usuário é ADMIN desta org?").
     */
    fun findByUserIdAndOrganizationId(userId: String, organizationId: String): OrganizationMember? {
        val snapshot = collection
            .whereEqualTo("userId", userId)
            .whereEqualTo("organizationId", organizationId)
            .limit(1)
            .get().get()

        return snapshot.documents.firstOrNull()?.toObject(OrganizationMember::class.java)?.copy(id = snapshot.documents.first().id)
    }

    /**
     * Retorna todas as organizações das quais o usuário faz parte.
     */
    fun findAllByUserId(userId: String): List<OrganizationMember> {
        val snapshot = collection
            .whereEqualTo("userId", userId)
            .get()
            .get()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(OrganizationMember::class.java)?.copy(id = doc.id)
        }
    }

    /**
     * Retorna todos os membros de uma organização.
     */
    fun findAllByOrganizationId(organizationId: String): List<OrganizationMember> {
        val snapshot = collection
            .whereEqualTo("organizationId", organizationId)
            .get()
            .get()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(OrganizationMember::class.java)?.copy(id = doc.id)
        }
    }

    /**
     * Busca membros de uma organização filtrando pelo cargo (Role).
     */
    fun findByOrganizationIdAndRole(organizationId: String, role: Role): List<OrganizationMember> {
        val snapshot = collection
            .whereEqualTo("organizationId", organizationId)
            .whereEqualTo("role", role.name)
            .get()
            .get()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(OrganizationMember::class.java).copy(id = doc.id)
        }
    }

    /**
     * Atualiza apenas o campo 'faceImageId'.
     */
    fun updateFaceImageId(id: String, newFaceImagePublicId: String) {
        collection.document(id).update("faceImageId", newFaceImagePublicId).get()
    }

    /**
     * Atualiza o Status (ex: banir um membro ou aprovar um pendente).
     */
    fun updateStatus(id: String, status: MemberStatus) {
        collection.document(id).update("status", status.name).get()
    }

    /**
     * Atualiza a Role (ex: promover de MEMBER para VALIDATOR).
     */
    fun updateRole(id: String, role: Role) {
        collection.document(id).update("role", role.name).get()
    }

    fun delete(id: String) {
        collection.document(id).delete().get()
    }
}