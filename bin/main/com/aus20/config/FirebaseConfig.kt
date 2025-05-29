package com.aus20.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct
import java.io.FileInputStream

@Configuration
class FirebaseConfig {

    @PostConstruct
    fun initFirebase() {
        val serviceAccount = FileInputStream("src/main/resources/personal-travel-assistan-c2192-firebase-adminsdk-fbsvc-8c7c11f8a0.json")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
            println("Firebase Admin initialized successfully")
        }
    }
}
