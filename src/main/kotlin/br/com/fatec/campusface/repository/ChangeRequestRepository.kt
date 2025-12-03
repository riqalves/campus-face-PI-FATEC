package br.com.fatec.campusface.repository

import br.com.fatec.campusface.models.ChangeRequest
import br.com.fatec.campusface.models.RequestStatus
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import org.springframework.stereotype.Repository

@Repository
class ChangeRequestRepository(private val firestore: Firestore) {
    private val collection = firestore.collection("changeRequests")

    fun save(request: ChangeRequest): ChangeRequest {
        println("DEBUG [Repo] - Salvando ChangeRequest. ID recebido: '${request.id}'")

        val docRef = if (request.id.isNotEmpty()) {
            println("DEBUG [Repo] - ID existente detectado. Atualizando documento: ${request.id}")
            collection.document(request.id)
        } else {
            println("DEBUG [Repo] - ID vazio. Gerando NOVO documento.")
            collection.document()
        }

        val requestWithId = request.copy(id = docRef.id)

        docRef.set(requestWithId).get()

        println("DEBUG [Repo] - Salvo com sucesso no Firestore. ID Final: ${docRef.id} - Status: ${requestWithId.status}")
        return requestWithId
    }

    fun findById(id: String): ChangeRequest? {
        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(ChangeRequest::class.java) else null
    }

    fun findPendingByOrganizationId(orgId: String): List<ChangeRequest> {
        println("DEBUG [Repo] - Buscando pendentes para Org: $orgId")
        val results = collection
            .whereEqualTo("organizationId", orgId)
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .get().get()
            .toObjects(ChangeRequest::class.java)

        println("DEBUG [Repo] - Encontrados ${results.size} pedidos pendentes.")
        return results
    }

    fun findByUserId(userId: String): List<ChangeRequest> {
        return collection
            .whereEqualTo("userId",userId)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .get().get()
            .toObjects(ChangeRequest::class.java)
    }

    fun delete(id: String) {
        collection.document(id).delete().get()
    }
}