package br.com.fatec.campusface.repository

import br.com.fatec.campusface.models.User
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FieldPath
import org.springframework.stereotype.Repository


@Repository
class UserRepository(private val firestore: Firestore) {

    private val collection = firestore.collection("users")

    fun save(user: User): User {
        val docRef = if (user.id.isNotEmpty()) {
            collection.document(user.id)
        } else {
            collection.document()
        }

        val userWithId = user.copy(id = docRef.id)

        
        docRef.set(userWithId).get()

        return userWithId
    }

    fun findAll(): List<User> {
        val snapshot = collection.get().get()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(id = doc.id)
        }
    }

    fun findById(id: String): User? {
        val documentSnapshot = collection.document(id).get().get()

        if (documentSnapshot.exists()) {
            val userObject = documentSnapshot.toObject(User::class.java)

            return userObject?.copy(id = documentSnapshot.id)
        }

        return null
    }
    fun findByEmail(email: String): User? {
        val snapshot = collection
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .get()

        if (snapshot.isEmpty) {
            return null
        }

        val document = snapshot.documents.first()
        return document.toObject(User::class.java).copy(id = document.id)
    }


    fun delete(id: String): Boolean {
        return try {
            collection.document(id).delete().get()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Busca uma lista de usuários com base em uma lista de IDs.
     * @param userIds A lista de IDs dos usuários a serem buscados.
     * @return Uma lista de objetos User correspondentes aos IDs encontrados.
     */
    fun findAllByIds(userIds: List<String>): List<User> {
        if (userIds.isEmpty()) {
            return emptyList()
        }

        val snapshot = collection
            .whereIn(FieldPath.documentId(), userIds)
            .get()
            .get()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(User::class.java)?.copy(id = document.id)
        }
    }

}
