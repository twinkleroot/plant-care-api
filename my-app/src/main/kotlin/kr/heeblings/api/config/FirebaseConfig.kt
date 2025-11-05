package kr.heeblings.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kr.heeblings.common.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.io.IOException
import javax.annotation.PostConstruct

@Configuration
class FirebaseConfig {

    // 1. 화초 관리 앱 (Project A) 서비스 계정 주입
    @Value("\${firebase.plant-care-service-account-path}")
    private lateinit var plantCareResource: Resource

    // 2. 루틴 관리 앱 (Project B) 서비스 계정 주입
    @Value("\${firebase.routine-manager-service-account-path}")
    private lateinit var routineManagerResource: Resource

    // 3. 앱 인스턴스에 사용할 고유 이름 (Service 코드에서 이 이름을 사용합니다)
    companion object {
        const val PLANT_APP_NAME = "plantApp"
        const val ROUTINE_APP_NAME = "routineApp"
    }

    @PostConstruct
    fun initialize() {
        val initializedApps = FirebaseApp.getApps().map { it.name }.toSet()

        try {
            // 1. 화초 관리 앱 초기화 (Project A)
            if (!initializedApps.contains(PLANT_APP_NAME)) {
                val plantOptions = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(plantCareResource.inputStream))
                    .build()
                FirebaseApp.initializeApp(plantOptions, PLANT_APP_NAME)
                log.info("FirebaseApp [${PLANT_APP_NAME}] initialized successfully.")
            }

            // 2. 루틴 관리 앱 초기화 (Project B)
            if (!initializedApps.contains(ROUTINE_APP_NAME)) {
                val routineOptions = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(routineManagerResource.inputStream))
                    .build()
                FirebaseApp.initializeApp(routineOptions, ROUTINE_APP_NAME)
                log.info("FirebaseApp [${ROUTINE_APP_NAME}] initialized successfully.")
            }

        } catch (e: IOException) {
            log.error("Failed to read Firebase service account file: ${e.message}", e)
            throw IllegalStateException("Firebase App initialization failed due to file error", e)
        } catch (e: Exception) {
            log.error("Failed to initialize Firebase Apps: ${e.message}", e)
            throw IllegalStateException("Firebase App initialization failed", e)
        }
    }
}