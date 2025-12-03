package br.com.fatec.campusface.repository

import br.com.fatec.campusface.models.Organization
import br.com.fatec.campusface.models.OrganizationMember
import com.google.cloud.firestore.FieldPath
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import org.springframework.stereotype.Repository


@Repository
class OrganizationRepository(private val firestore: Firestore) {

    private val collection = firestore.collection("organizations")


    fun save(organization: Organization): Organization {
        val docRef = collection.document()

        val organizationWithId = organization.copy(id = docRef.id)
        docRef.set(organizationWithId).get()
        return organizationWithId
    }


    fun findById(id: String): Organization? {
        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(Organization::class.java) else null
    }

    fun findByHubCode(hubCode: String): Organization? {
        val snapshot = collection.whereEqualTo("hubCode", hubCode)
            .limit(1)
            .get().get()
        return snapshot.documents.firstOrNull()?.toObject(Organization::class.java)
    }

    fun findAll(): List<Organization> {
        val docs = collection.get().get().documents
        return docs.mapNotNull { it.toObject(Organization::class.java) }
    }


    fun findAllByIds(ids: List<String>): List<Organization> {
        if (ids.isEmpty()) return emptyList()

        val snapshot = collection
            .whereIn(FieldPath.documentId(), ids)
            .get().get()
        return snapshot.toObjects(Organization::class.java)

    }

    fun update(id: String, organization: Organization): Organization? {
        val docRef = collection.document(id)
        // Verifica se o documento existe antes de tentar atualizar
        if (docRef.get().get().exists()) {
            docRef.set(organization).get()
            return organization
        }
        return null
    }

    fun delete(id: String): Boolean {
        val docRef = collection.document(id)
        val doc = docRef.get().get()
        return if (doc.exists()) {
            docRef.delete().get()
            true
        } else {
            false
        }
    }

    fun addMemberToOrganization(organizationId: String, memberId: String) {
        collection.document(organizationId).update("memberIds", FieldValue.arrayUnion(memberId))
    }

    fun addValidatorToOrganization(organizationId: String, validatorId: String) {
        collection.document(organizationId).update("validatorIds", FieldValue.arrayUnion(validatorId))
    }

    fun addAdminToOrganization(organizationId: String, adminId: String) {
        collection.document(organizationId).update("adminIds", FieldValue.arrayUnion(adminId))
    }

}
