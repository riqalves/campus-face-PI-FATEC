package br.com.fatec.campusface.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException


@Service
class CloudinaryService(
    private val cloudinary: Cloudinary,
    private val httpClient: OkHttpClient
) {


    fun upload(file: MultipartFile): Map<String, String> {
        // Esta chamada usa o array de bytes do MultipartFile
        return upload(file.bytes)
    }

    /**
     * Versão 2 (NOVA): Faz o upload a partir de um ByteArray.
     * É esta versão que o seu UserService irá chamar com a imagem processada.
     */
    fun upload(imageBytes: ByteArray): Map<String, String> {
        // O uploader do Cloudinary aceita um ByteArray diretamente.
        val result = cloudinary.uploader().upload(imageBytes, ObjectUtils.emptyMap())

        return mapOf(
            "secure_url" to (result["secure_url"] as String),
            "public_id" to (result["public_id"] as String)
        )
    }

    /**
     * Deleta uma imagem do Cloudinary usando seu public_id.
     */
    fun delete(publicId: String) {
        try {
            // O método 'destroy' é usado para deletar um recurso
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
            println("DEBUG - Imagem com public_id '$publicId' deletada do Cloudinary.")
        } catch (e: Exception) {
            println("ERRO - Falha ao deletar a imagem '$publicId' do Cloudinary: ${e.message}")

        }
    }

    /**
     * Gera uma URL assinada e temporária para um recurso do Cloudinary.
     * A URL expira após um tempo pré-configurado (padrão de 1 hora).
     * @param publicId O identificador único do arquivo no Cloudinary.
     * @return Uma URL completa e segura que expira.
     */
    fun generateSignedUrl(publicId: String): String {
        // Você pode configurar diversas opções, como tempo de expiração, etc.
        // Por padrão, a URL assinada tem validade de 1 hora.
        return cloudinary.url()
            .secure(true) // Garante que a URL seja HTTPS
            .signed(true) // Gera a assinatura criptográfica (a parte mais importante)
            .generate(publicId)
    }

    /**
     * NOVO: Baixa o conteúdo de uma imagem a partir de uma URL.
     * @param url A URL completa (ex: a URL assinada do Cloudinary).
     * @return Um ByteArray com os dados da imagem.
     */
    fun downloadImageFromUrl(url: String): ByteArray {
        println("DEBUG - Baixando imagem de: $url")
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Falha ao baixar imagem do Cloudinary. Código: ${response.code}, URL: $url")
            }
            // Retorna os bytes do corpo da resposta
            return response.body!!.bytes()
        }
    }
}