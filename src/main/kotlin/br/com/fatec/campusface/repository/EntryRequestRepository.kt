package br.com.fatec.campusface.repository

import br.com.fatec.campusface.models.EntryRequest
import br.com.fatec.campusface.models.RequestStatus
import br.com.fatec.campusface.service.UserService
import com.google.cloud.firestore.Firestore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class EntryRequestRepository(private val firestore: Firestore) {

    @Autowired
    private lateinit var userService: UserService

    private val collection = firestore.collection("entryRequests")


    // Cria um novo pedido de entrada
    fun save(entryRequest: EntryRequest): EntryRequest {
        val docRef = collection.document()
        val requestWithId = entryRequest.copy(id = docRef.id)
        docRef.set(requestWithId).get()
        return requestWithId
    }

    // Buscar por ID
    fun findById(id: String): EntryRequest? {
        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(EntryRequest::class.java) else null
    }

    // Buscar todos os pedidos de uma organização dependendo do status
    fun findByOrganizationIdAndStatus(organizationId: String, status: RequestStatus): List<EntryRequest> {
        val snapshot = collection
            .whereEqualTo("organizationId", organizationId)
            .whereEqualTo("status", status.name) // Firestore salva Enums como String
            .get()
            .get()

        return snapshot.documents.mapNotNull { it.toObject(EntryRequest::class.java) }
    }

    fun updateStatus(id: String, newStatus: RequestStatus) {
        collection.document(id).update("status", newStatus.name).get()
    }

    // Se precisar deletar
    fun delete(id: String) {
        collection.document(id).delete().get()
    }

    // Busca todas as solicitações feitas por um usuário específico
    fun findByUserId(userId: String): List<EntryRequest> {
        val snapshot = collection
            .whereEqualTo("userId", userId)
            // .orderBy("requestedAt", Query.Direction.DESCENDING) // Opcional: ordenar por data se criar índice no Firestore
            .get()
            .get()

        return snapshot.documents.mapNotNull { it.toObject(EntryRequest::class.java) }
    }
}
