package br.com.fatec.campusface.repository

import br.com.fatec.campusface.models.ClientStatus
import br.com.fatec.campusface.models.RegisteredClient
import com.google.cloud.firestore.Firestore
import org.springframework.stereotype.Repository

@Repository
class RegisteredClientRepository(private val firestore: Firestore) {
    private val collection = firestore.collection("registeredClients")

    fun save(client: RegisteredClient): RegisteredClient {
        val docRef = if(client.id.isNotEmpty()) collection.document(client.id) else collection.document()
        val clientWithId = client.copy(id = docRef.id)
        docRef.set(clientWithId).get()
        return clientWithId
    }

    fun findById(id: String): RegisteredClient? {
        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(RegisteredClient::class.java) else null
    }

    fun findByOrganizaitonIdAndStatus(organizationId: String, status: ClientStatus): List<RegisteredClient> {
        val snapshot = collection
            .whereEqualTo("organizationId", organizationId)
            .whereEqualTo("status", status.name)
            .get().get()
        return snapshot.documents.mapNotNull{it.toObject(RegisteredClient::class.java)}
    }

    // Removido o parâmetro 'port'
    fun findByAddress(organizationId: String, ipAddress: String): RegisteredClient? {
        val snapshot = collection
            .whereEqualTo("organizationId", organizationId)
            .whereEqualTo("ipAddress", ipAddress)
            .limit(1).get().get()
        return snapshot.documents.firstOrNull()?.toObject(RegisteredClient::class.java)
    }

    // NOVO MÉTODO: Busca pela Identidade da Máquina
    fun findByMachineId(machineId: String): RegisteredClient? {
        val snapshot = collection
            .whereEqualTo("machineId", machineId)
            .limit(1)
            .get().get()
        return snapshot.documents.firstOrNull()?.toObject(RegisteredClient::class.java)
    }
}