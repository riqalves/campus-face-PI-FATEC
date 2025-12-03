package br.com.fatec.campusface.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfiguration {

    @Bean
    fun firebaseApp(): FirebaseApp {
        val serviceAccount = this::class.java.getResourceAsStream("/firebase.json")
            ?: throw IllegalStateException("Arquivo firebase.json não encontrado em resources")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        // Retorna a instância existente, se já houver
        return FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firestore(firebaseApp: FirebaseApp): Firestore {
        return FirestoreClient.getFirestore(firebaseApp)
    }
}
