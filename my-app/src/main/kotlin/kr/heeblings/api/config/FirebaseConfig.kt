package kr.heeblings.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import javax.annotation.PostConstruct

@Configuration
class FirebaseConfig {

    @Value("\${firebase.service-account-path}")
    private lateinit var serviceAccountResource: Resource

    @PostConstruct
    fun initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountResource.inputStream))
                .build()
            FirebaseApp.initializeApp(options)
        }
    }
}