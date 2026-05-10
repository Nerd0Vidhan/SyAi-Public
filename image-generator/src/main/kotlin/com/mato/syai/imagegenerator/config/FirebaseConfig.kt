package com.mato.syai.imagegenerator.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.context.annotation.Configuration
import java.io.InputStream
import javax.annotation.PostConstruct

@Configuration
class FirebaseConfig {

    @PostConstruct
    fun initialize() {
        try {
            val serviceAccount: InputStream? = this.javaClass.classLoader.getResourceAsStream("service-account.json")
            
            if (serviceAccount == null) {
                println("WARNING: service-account.json not found in resources. Firebase notifications will not work.")
                return
            }

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("Firebase initialized successfully.")
            }
        } catch (e: Exception) {
            println("ERROR: Failed to initialize Firebase: ${e.message}")
            e.printStackTrace()
        }
    }
}
