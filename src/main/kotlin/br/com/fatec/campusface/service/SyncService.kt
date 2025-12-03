package br.com.fatec.campusface.service

import br.com.fatec.campusface.models.ClientStatus
import br.com.fatec.campusface.models.RegisteredClient
import br.com.fatec.campusface.repository.RegisteredClientRepository
import br.com.fatec.campusface.repository.UserRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class SyncService(
    private val registeredClientRepository: RegisteredClientRepository,
    private val userRepository: UserRepository,
    private val cloudinaryService: CloudinaryService,
    private val httpClient: OkHttpClient
) {

    // --- UPSERT (CRIAR/ATUALIZAR) ---
    @Async
    fun syncNewMember(organizationId: String, userId: String) {
        val activeClients = registeredClientRepository.findByOrganizaitonIdAndStatus(organizationId, ClientStatus.ONLINE)

        if (activeClients.isEmpty()) {
            println("AVISO (Sync): Nenhum cliente Python online para o Hub $organizationId")
            return
        }

        val user = userRepository.findById(userId) ?: return
        val faceId = user.faceImageId ?: return

        val imageBytes: ByteArray
        try {
            val signedUrl = cloudinaryService.generateSignedUrl(faceId)
            imageBytes = cloudinaryService.downloadImageFromUrl(signedUrl)
        } catch (e: Exception) {
            println("ERRO (Sync): Falha ao baixar imagem: ${e.message}")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", userId)
            .addFormDataPart(
                "image",
                "face.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        activeClients.forEach { client ->
            sendUpsertRequest(client, requestBody)
        }
    }

    private fun sendUpsertRequest(client: RegisteredClient, requestBody: MultipartBody) {
        val url = "http://${client.ipAddress}/upsert"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("ngrok-skip-browser-warning", "true")
            .build()

        try {
            println("DEBUG (Sync Upsert): Enviando para ${client.name} ($url)...")
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    println("SUCESSO (Sync): Face sincronizada com ${client.name}.")
                } else {
                    println("ERRO (Sync): Cliente ${client.name} rejeitou (Code: ${response.code}).")
                }
            }
        } catch (e: IOException) {
            println("ERRO FATAL (Sync): Falha ao conectar com ${client.name}: ${e.message}")
            markClientAsUnreachable(client)
        } catch (e: Exception) {
            println("ERRO (Sync): Erro genérico: ${e.message}")
        }
    }

    // --- DELETE (REMOVER) ---

    @Async
    fun syncMemberDeletion(organizationId: String, userId: String) {
        // 1. Busca Clientes Online
        val activeClients = registeredClientRepository.findByOrganizaitonIdAndStatus(organizationId, ClientStatus.ONLINE)

        if (activeClients.isEmpty()) {
            println("AVISO (Sync Delete): Nenhum cliente online para o Hub $organizationId. A remoção ficará pendente (TODO: Full Sync).")
            return
        }

        println("DEBUG (Sync Delete): Iniciando remoção do usuário $userId em ${activeClients.size} totens.")

        // 2. Envia comando de delete para cada um
        activeClients.forEach { client ->
            sendDeleteRequest(client, userId)
        }
    }

    private fun sendDeleteRequest(client: RegisteredClient, userId: String) {
        // Monta a URL: http://IP/delete/USER_ID
        val url = "http://${client.ipAddress}/delete/$userId"

        val request = Request.Builder()
            .url(url)
            .delete() // Verbo HTTP DELETE
            .header("ngrok-skip-browser-warning", "true")
            .build()

        try {
            println("DEBUG (Sync Delete): Removendo usuário $userId de ${client.name} ($url)...")

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    println("SUCESSO (Sync Delete): Usuário removido do cliente ${client.name}.")
                } else {
                    println("ERRO (Sync Delete): Cliente ${client.name} falhou ao remover (Code: ${response.code}).")
                }
            }
        } catch (e: IOException) {
            println("ERRO FATAL (Sync Delete): Falha ao conectar com ${client.name}: ${e.message}")
            markClientAsUnreachable(client)
        } catch (e: Exception) {
            println("ERRO (Sync Delete): Erro genérico: ${e.message}")
        }
    }

    private fun markClientAsUnreachable(client: RegisteredClient) {
        val updatedClient = client.copy(status = ClientStatus.UNREACHABLE)
        registeredClientRepository.save(updatedClient)
    }
}