package br.com.fatec.campusface.service

import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
class ImageProcessingService {

    private val targetSize = 1024 // Define o tamanho máximo (largura ou altura)
    private val outputQuality = 0.85f // Define a qualidade do JPG (85%)

    /**
     * Processa e otimiza uma imagem para envio a uma API de reconhecimento facial.
     */
    fun processImageForApi(imageFile: MultipartFile): ByteArray {
        return processImageBytes(imageFile.bytes)
    }

    /**
     * Sobrecarga do método para processar uma imagem que já está em um array de bytes.
     */
    fun processImageBytes(imageBytes: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val originalSizeKB = imageBytes.size / 1024

        Thumbnails.of(ByteArrayInputStream(imageBytes))
            .size(targetSize, targetSize)
            .outputFormat("jpg")
            .outputQuality(outputQuality)
            .toOutputStream(outputStream)

        val processedBytes = outputStream.toByteArray()
        val processedSizeKB = processedBytes.size / 1024

        // Adicione este log
        println("DEBUG (ImageProcessing): Imagem otimizada. Tamanho original: $originalSizeKB KB -> Tamanho final: $processedSizeKB KB")

        return processedBytes
    }
}