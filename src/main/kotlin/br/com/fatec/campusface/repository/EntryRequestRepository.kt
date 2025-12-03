package br.com.fatec.campusface.repository

import br.com.fatec.campusface.models.EntryRequest
import br.com.fatec.campusface.models.RequestStatus
import br.com.fatec.campusface.service.UserService
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class EntryRequestRepository(private val firestore: Firestore) {

    private val collection = firestore.collection("entryRequests")


    fun save(entryRequest: EntryRequest): EntryRequest {
        val docRef = collection.document()
        val requestWithId = entryRequest.copy(id = docRef.id)
        docRef.set(requestWithId).get()
        return requestWithId
    }

    fun findById(id: String): EntryRequest? {
        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(EntryRequest::class.java) else null
    }

    fun findByOrganizationIdAndStatus(organizationId: String, status: RequestStatus): List<EntryRequest> {
        val snapshot = collection
            .whereEqualTo("organizationId", organizationId)
            .whereEqualTo("status", status.name)
            .get()
            .get()

        return snapshot.documents.mapNotNull { it.toObject(EntryRequest::class.java) }
    }

    fun updateStatus(id: String, newStatus: RequestStatus) {
        collection.document(id).update("status", newStatus.name).get()
    }

    fun delete(id: String) {
        collection.document(id).delete().get()
    }

    fun findByUserId(userId: String): List<EntryRequest> {
        val snapshot = collection
            .whereEqualTo("userId", userId)
             .orderBy("requestedAt", Query.Direction.DESCENDING)
            .get()
            .get()

        return snapshot.documents.mapNotNull { it.toObject(EntryRequest::class.java) }
    }
}
