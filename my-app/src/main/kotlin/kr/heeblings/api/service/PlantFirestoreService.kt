package kr.heeblings.api.service

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.SetOptions
import kr.heeblings.api.domain.PlantPushMessage
import kr.heeblings.api.domain.PlantUser
import kr.heeblings.common.utils.log
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit
import kr.heeblings.api.dto.PlantWikiResponse
import kr.heeblings.api.exception.ResourceNotFoundException
import java.time.LocalDateTime

@Service
class PlantFirestoreService (
    private val firestore: Firestore
) {
    // Firestore 컬렉션 경로
    private val USERS_COLLECTION = "users"
    private val PLANTS_COLLECTION = "plants"
    private val WIKI_COLLECTION = "plantWiki"
    private val PUSH_MESSAGES_COLLECTION = "pushMessages"

    // ----------------------------------------------------
    // 인증 및 사용자 관리
    // ----------------------------------------------------

    /**
     * 카카오 로그인 성공 후 Firestore에 사용자 정보를 저장/갱신합니다.
     * @return DB에 저장된 사용자 ID (String)
     */
    fun saveOrUpdateUser(kakaoId: Long, nickname: String?, fcmToken: String?): String {
        val userId = kakaoId.toString() // Firestore에서는 Kakao ID를 UID로 사용
        val userDoc = firestore.collection(USERS_COLLECTION).document(userId)

        val userData = mutableMapOf<String, Any?>()
        userData["nickname"] = nickname ?: "User"
        if (fcmToken != null) {
            userData["fcmToken"] = fcmToken
        }
        userData["kakaoId"] = kakaoId
        userData["updatedAt"] = Date()

        userDoc.set(userData, SetOptions.merge()).get(10, TimeUnit.SECONDS)
        log.info("Firestore user updated/created: userId={}", userId)

        return userId
    }

    /**
     * Firestore에서 사용자 데이터와 연관된 모든 데이터를 삭제합니다.
     */
    fun deleteUser(userId: String) {
        val userRef = firestore.collection(USERS_COLLECTION).document(userId)

        // 1. 하위 컬렉션 삭제 (plants)
        deleteCollection(userRef.collection(PLANTS_COLLECTION), 50)

        // 2. 하위 컬렉션 삭제 (pushMessages)
        deleteCollection(userRef.collection(PUSH_MESSAGES_COLLECTION), 50)

        // 3. 사용자 문서 삭제
        userRef.delete().get()
        log.info("User and all subcollections deleted for userId: {}", userId)
    }

    // 컬렉션 삭제 헬퍼 함수
    private fun deleteCollection(collection: com.google.cloud.firestore.CollectionReference, batchSize: Int) {
        try {
            val future = collection.limit(batchSize).get()
            val documents = future.get().documents
            for (document in documents) {
                document.reference.delete()
            }
            if (documents.size >= batchSize) {
                deleteCollection(collection, batchSize) // 남은게 있으면 재귀 호출
            }
        } catch (e: Exception) {
            log.error("Error deleting collection: " + e.message)
        }
    }

    fun findUserById(userId: String): PlantUser? {
        val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().get(5, TimeUnit.SECONDS)
        return if (userDoc.exists()) {
            PlantUser(
                userId = userId.toLongOrNull() ?: 0L,
                kakaoId = userDoc.getLong("kakaoId") ?: 0L,
                nickname = userDoc.getString("nickname"),
                fcmToken = userDoc.getString("fcmToken")
            )
        } else {
            null
        }
    }

    // ----------------------------------------------------
    // 위키 데이터 관리 (PlantTypeWiki 대체)
    // ----------------------------------------------------

    /**
     * PlantTypeWiki 테이블 대신 Firestore에서 모든 위키 데이터를 조회합니다.
     */
    fun getPlantWikiList(): List<PlantWikiResponse> {
        val query = firestore.collection(WIKI_COLLECTION)
            .orderBy("plantTypeName", Query.Direction.ASCENDING)
            .get()
            .get(10, TimeUnit.SECONDS)

        return query.documents.map { doc ->
            PlantWikiResponse(
                plantTypeName = doc.getString("plantTypeName") ?: "",
                wateringCycleDays = doc.getLong("wateringCycleDays")?.toInt(),
                description = doc.getString("description"),
                careInfo = doc.getString("careInfo")
            )
        }
    }

    fun findWikiByTypeName(typeName: String): PlantWikiResponse? {
        val doc = firestore.collection(WIKI_COLLECTION)
            .whereEqualTo("plantTypeName", typeName)
            .limit(1)
            .get()
            .get(5, TimeUnit.SECONDS)
            .documents.firstOrNull()

        return doc?.let { PlantWikiResponse(
            plantTypeName = it.getString("plantTypeName") ?: "",
            wateringCycleDays = it.getLong("wateringCycleDays")?.toInt(),
            description = it.getString("description"),
            careInfo = it.getString("careInfo")
        )}
    }


    // ----------------------------------------------------
    // 식물 정보 관리
    // ----------------------------------------------------

    /**
     * 식물 정보를 Firestore에 저장하고 ID를 반환합니다.
     */
    fun savePlant(userId: String, data: Map<String, Any?>): String {
        val newPlantRef = firestore.collection(USERS_COLLECTION).document(userId).collection(PLANTS_COLLECTION).document()
        newPlantRef.set(data).get(10, TimeUnit.SECONDS)
        log.info("Plant saved: userId={}, plantId={}", userId, newPlantRef.id)
        return newPlantRef.id
    }

    /**
     * 식물 상세 정보를 Firestore에서 조회합니다.
     */
    fun findPlantById(userId: String, plantId: String): Map<String, Any?>? {
        val plantDoc = firestore.collection(USERS_COLLECTION).document(userId).collection(PLANTS_COLLECTION).document(plantId).get().get(10, TimeUnit.SECONDS)
        return if (plantDoc.exists()) {
            val data = plantDoc.data
            data?.put("plantId", plantDoc.id) // 문서 ID를 plantId로 추가
            data
        } else {
            null
        }
    }

    /**
     * 식물 리스트를 Firestore에서 조회합니다.
     */
    fun findPlantList(userId: String, page: Int, size: Int, sortField: String = "createdAt", isAsc: Boolean = false): List<Map<String, Any?>> {
        val query = firestore.collection(USERS_COLLECTION).document(userId).collection(PLANTS_COLLECTION)

        val direction = if (isAsc) Query.Direction.ASCENDING else Query.Direction.DESCENDING

        val result = query
            .orderBy(sortField, direction)
            .offset(page * size)
            .limit(size)
            .get()
            .get(10, TimeUnit.SECONDS)

        return result.documents.map { doc ->
            val data = doc.data.toMutableMap()
            data["plantId"] = doc.id
            data
        }
    }

    /**
     * 식물 정보 수정 및 물주기 처리 (Firestore)
     */
    fun updatePlant(userId: String, plantId: String, data: Map<String, Any?>) {
        val plantDoc = firestore.collection(USERS_COLLECTION).document(userId).collection(PLANTS_COLLECTION).document(plantId)
        plantDoc.update(data).get(10, TimeUnit.SECONDS)
    }

    /**
     * 식물 삭제 (Firestore)
     */
    fun deletePlant(userId: String, plantId: String) {
        val plantDoc = firestore.collection(USERS_COLLECTION).document(userId).collection(PLANTS_COLLECTION).document(plantId)
        plantDoc.delete().get(10, TimeUnit.SECONDS)
    }

    /**
     * 식물 정보의 ImageStatus를 업데이트합니다. (비동기 처리 완료 후)
     */
    fun updatePlantImageStatus(userId: String, plantId: String, imageUrl: String?, status: String) {
        val plantDoc = firestore.collection(USERS_COLLECTION).document(userId).collection(PLANTS_COLLECTION).document(plantId)

        val updates = mutableMapOf<String, Any?>()
        updates["imageStatus"] = status
        updates["updatedAt"] = Date()

        if (imageUrl != null) {
            updates["imageUrl"] = imageUrl
        }

        plantDoc.update(updates).get(10, TimeUnit.SECONDS)
    }

    /**
     * 물주기 알림 스케줄러를 위해 오늘 물 줘야 하는 사용자 목록을 조회합니다.
     */
    fun findUsersToWaterToday(): List<PlantUser> {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.of("Asia/Seoul"))
            .toInstant().toEpochMilli()

        // nextWateringDateMillis만으로 조회한 후, 아래 코드에서 User 정보를 조회할 때 FCM 토큰 유무를 확인합니다.
        // 참고: 이 쿼리를 실행할 때 "The query requires an index" 에러가 또 발생할 수 있습니다.
        // 그럴 경우 에러 로그에 나오는 URL을 클릭하여 'nextWateringDateMillis'에 대한 단일 필드 인덱스(Collection Group용)를 생성해주세요.
        val query = firestore.collectionGroup(PLANTS_COLLECTION)
            .whereLessThanOrEqualTo("nextWateringDate", todayStart)
//            .whereGreaterThan("fcmToken", "") // FCM 토큰이 있는 사용자만 필터링
            .limit(500) // 대량 처리를 위해 제한
            .get()
            .get(30, TimeUnit.SECONDS)

        val userMap = mutableMapOf<String, PlantUser>()

        query.documents.forEach { plantDoc ->
            val userId = plantDoc.reference.parent.parent?.id // users/{uid}/plants/{pid} 구조에서 uid를 추출

            // 사용자의 모든 정보를 Firestore에서 다시 조회 (최신 토큰 정보 포함)
            val userDoc = userId?.let { firestore.collection(USERS_COLLECTION).document(it).get().get(5, TimeUnit.SECONDS) }
            if (userDoc != null) {
                if (userDoc.exists()) {
                    val fcmToken = userDoc.getString("fcmToken")
                    val kakaoId = userId.toLongOrNull()

                    if (kakaoId != null && !fcmToken.isNullOrBlank()) {
                        userMap[userId] = PlantUser(
                            userId = kakaoId,
                            kakaoId = kakaoId,
                            nickname = userDoc.getString("nickname"),
                            fcmToken = fcmToken
                        )
                    }
                }
            }
        }
        return userMap.values.toList()
    }

    /**
     * 푸시 메시지를 Firestore에 저장합니다.
     */
    fun savePushMessage(user: PlantUser, title: String, body: String) {
        val userId = user.kakaoId.toString()
        val messageDoc = firestore.collection(USERS_COLLECTION).document(userId).collection(PUSH_MESSAGES_COLLECTION).document()

        val messageData = mapOf(
            "title" to title,
            "body" to body,
            "isRead" to false,
            "createdAt" to Date()
        )
        messageDoc.set(messageData).get() // 비동기 대기
        log.info("Push message saved for userId: {}", userId)
    }

    /**
     * 읽지 않은 푸시 메시지 목록 조회 (최신순 10개)
     */
    fun findUnreadPushMessages(userId: String): List<PlantPushMessage> {
        val query = firestore.collection(USERS_COLLECTION).document(userId).collection(PUSH_MESSAGES_COLLECTION)
            .whereEqualTo("isRead", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .get(10, TimeUnit.SECONDS)

        return query.documents.map { doc ->
            val data = doc.data
            PlantPushMessage(
                messageId = doc.id,
                title = data["title"] as? String ?: "",
                body = data["body"] as? String ?: "",
                isRead = data["isRead"] as? Boolean ?: false,
                createdAt = toLocalDateTime(data["createdAt"]) ?: LocalDateTime.now()
            )
        }
    }

    /**
     * 메시지 읽음 처리
     */
    fun markPushMessageAsRead(userId: String, messageId: String): PlantPushMessage {
        val docRef = firestore.collection(USERS_COLLECTION).document(userId).collection(PUSH_MESSAGES_COLLECTION).document(messageId)

        // isRead 필드 업데이트
        docRef.update("isRead", true).get(10, TimeUnit.SECONDS)

        // 업데이트된 문서 조회 및 반환
        val snapshot = docRef.get().get(10, TimeUnit.SECONDS)
        val data = snapshot.data ?: throw ResourceNotFoundException("Message not found with id: $messageId")

        return PlantPushMessage(
            messageId = snapshot.id,
            title = data["title"] as? String ?: "",
            body = data["body"] as? String ?: "",
            isRead = data["isRead"] as? Boolean ?: true,
            createdAt = toLocalDateTime(data["createdAt"]) ?: LocalDateTime.now()
        )
    }

    /**
     * 메시지 삭제
     */
    fun deletePushMessage(userId: String, messageId: String) {
        firestore.collection(USERS_COLLECTION).document(userId).collection(PUSH_MESSAGES_COLLECTION).document(messageId)
            .delete()
            .get(10, TimeUnit.SECONDS)
    }

    // Date/Timestamp -> LocalDateTime 변환 유틸
    private fun toLocalDateTime(obj: Any?): LocalDateTime? {
        return when (obj) {
            is com.google.cloud.Timestamp -> obj.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            is Date -> obj.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            else -> null
        }
    }
}