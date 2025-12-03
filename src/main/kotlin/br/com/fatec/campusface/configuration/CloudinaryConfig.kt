package br.com.fatec.campusface.configuration

import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CloudinaryConfig {

    @Value("\${cloudinary.url}")
    private lateinit var cloudinaryUrl: String


    @Bean
    fun cloudinary(): Cloudinary {
        return Cloudinary(cloudinaryUrl)
    }
}