package kr.heeblings.api.controller

import com.google.cloud.firestore.Firestore
import kr.heeblings.api.service.PlantFirestoreService
import kr.heeblings.common.utils.log
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@RestController
@RequestMapping("/migration")
class MigrationController(
    private val firestore: Firestore,
    private val firestoreService: PlantFirestoreService
) {
    // 데이터 클래스 정의
    data class WikiData(
        val plantTypeName: String,
        val wateringCycleDays: Int,
        val description: String,
        val careInfo: String
    )

    // 사용자 데이터 클래스
    data class UserData(
        val kakaoId: Long,
        val nickname: String,
        val fcmToken: String
    )

    // 식물 데이터 클래스 (CSV 구조 반영)
    data class PlantCsvData(
        val userId: Long,
        val nickname: String?,
        val imageUrl: String?,
        val imageStatus: String,
        val plantType: String?,
        val startDate: String,
        val lastWateredDate: String?,
        val lastRepottedDate: String?,
        val wateringCycleDays: Int?,
        val nextWateringDate: String?,
        val description: String?,
        val careInfo: String?,
        val createdAt: String,
        val updatedAt: String
    )

    // 푸시 메시지 데이터 클래스
    data class PushMessageCsvData(
        val userId: Long,
        val title: String,
        val body: String,
        val isRead: Int, // CSV에서 0 또는 1로 옴
        val createdAt: String
    )

    @GetMapping("/wiki")
    fun migrateWikiData(): ResponseEntity<String> {
        val collection = firestore.collection("plantWiki")
        val batch = firestore.batch()

        // 기존 100종의 식물 데이터 리스트
        val plants = listOf(
            // --- 기존 60종 ---
            WikiData("몬스테라 델리시오사", 10, "독특하게 찢어진 잎이 매력적인 인테리어 식물입니다.", "반양지에서 잘 자라며, 흙이 마르면 물을 듬뿍 주세요. 과습에 주의하세요."),
            WikiData("산세베리아", 20, "음이온을 방출하고 공기 정화 능력이 뛰어난 생명력 강한 식물입니다.", "빛이 적은 곳에서도 잘 견디지만, 밝은 간접광에서 더 건강하게 자랍니다. 과습은 뿌리를 썩게 하니 주의하세요."),
            WikiData("금전수 (돈나무)", 15, "돈과 행운을 불러온다고 알려져 개업 선물로 인기가 많습니다.", "반음지에서도 잘 자라며 건조에 강합니다. 흙이 완전히 말랐을 때 물을 주는 것이 좋습니다."),
            WikiData("스투키", 25, "산세베리아의 한 종류로, 공기 정화 능력이 뛰어나고 관리가 쉽습니다.", "건조에 매우 강하므로 물을 자주 주지 마세요. 한 달에 한 번 정도가 적당합니다."),
            WikiData("고무나무", 12, "윤기나는 넓은 잎과 미세먼지 제거 효과로 사랑받는 식물입니다.", "밝은 간접광을 좋아하며, 흙 표면이 말랐을 때 물을 충분히 줍니다."),
            WikiData("스킨답서스", 7, "생명력이 강해 초보자도 쉽게 키울 수 있는 대표적인 실내 식물입니다.", "반음지나 간접광 어디서든 잘 자랍니다. 흙이 마르면 물을 주세요. 수경재배도 가능합니다."),
            WikiData("아레카야자", 7, "가습 효과가 뛰어나고 이국적인 분위기를 연출하는 야자나무입니다.", "밝은 간접광을 선호하며, 흙이 마르지 않도록 유지하는 것이 중요합니다. 잎에 자주 분무해주세요."),
            WikiData("테이블야자", 10, "이름처럼 책상이나 테이블 위에 올려놓기 좋은 작은 크기의 야자입니다.", "반음지에서도 잘 자라 관리가 용이합니다. 흙 표면이 말랐을 때 물을 주세요."),
            WikiData("관음죽", 9, "동양적인 매력이 있고 병충해에 강한 공기 정화 식물입니다.", "반음지나 간접광에서 잘 자라며, 물을 좋아하는 편이므로 흙을 촉촉하게 유지해주세요."),
            WikiData("보스턴고사리", 5, "풍성한 잎이 매력적이며, 가습 효과와 포름알데히드 제거 능력이 뛰어납니다.", "높은 습도를 좋아하므로 흙을 항상 촉촉하게 유지하고 잎에 자주 분무해주세요. 반음지에서 키우세요."),
            WikiData("아이비", 7, "덩굴성 식물로 벽을 타거나 화분에 늘어뜨려 키우기 좋습니다.", "밝은 곳을 좋아하지만 직사광선은 피해주세요. 흙 표면이 마르면 물을 줍니다."),
            WikiData("스파티필름", 8, "흰색의 아름다운 꽃과 뛰어난 공기 정화 능력으로 인기 있는 식물입니다.", "반음지에서 잘 자라며, 물을 좋아하므로 흙이 마르지 않게 관리해주세요."),
            WikiData("안스리움", 10, "하트 모양의 붉은색 포엽이 인상적인 관상용 식물입니다.", "밝은 간접광과 높은 습도를 좋아합니다. 흙 표면이 말랐을 때 물을 주세요."),
            WikiData("칼랑코에", 14, "다채로운 색상의 꽃을 오랫동안 피우는 다육식물의 일종입니다.", "햇빛을 매우 좋아합니다. 건조에 강하므로 흙이 완전히 마른 후에 물을 주세요."),
            WikiData("장미허브", 7, "장미와 허브 향이 섞인 듯한 향기와 두툼한 잎이 특징입니다.", "햇빛을 좋아하고 통풍이 잘 되는 곳에서 키우세요. 흙이 마르면 물을 줍니다."),
            WikiData("로즈마리", 6, "상쾌한 향기가 매력적인 허브로, 요리에도 활용할 수 있습니다.", "햇빛이 잘 들고 통풍이 잘 되는 곳을 좋아합니다. 흙이 말랐을 때 물을 주세요."),
            WikiData("라벤더", 8, "심신 안정에 도움을 주는 향기로운 보라색 꽃을 피우는 허브입니다.", "햇빛과 통풍을 매우 중요하게 생각합니다. 건조하게 관리하는 것이 좋습니다."),
            WikiData("싱고니움", 9, "화살촉 모양의 잎이 특징이며, 성장 속도가 빠르고 키우기 쉽습니다.", "반음지나 밝은 간접광에서 잘 자랍니다. 흙 표면이 말랐을 때 물을 주세요."),
            WikiData("필로덴드론 셀렘", 10, "크고 깊게 갈라진 잎이 이국적인 분위기를 연출하는 식물입니다.", "넓은 공간을 차지하며, 밝은 간접광을 좋아합니다. 흙이 말랐을 때 물을 줍니다."),
            WikiData("뱅갈고무나무", 12, "연두색 잎맥이 아름다운 고무나무의 한 종류입니다.", "밝은 간접광을 선호하며, 통풍이 잘 되는 곳에서 키우세요. 흙 표면이 말랐을 때 물을 줍니다."),
            WikiData("떡갈고무나무", 14, "떡갈나무 잎을 닮은 크고 멋진 잎을 가진 인기 인테리어 식물입니다.", "밝은 간접광과 통풍이 중요합니다. 흙이 완전히 마른 후에 물을 주세요."),
            WikiData("해피트리 (파키라)", 15, "행운을 가져다준다고 알려져 있으며, 굵은 줄기와 풍성한 잎이 특징입니다.", "반양지에서 잘 자라며, 건조에 강한 편입니다. 흙이 마르면 물을 줍니다."),
            WikiData("녹보수", 10, "보석처럼 반짝이는 녹색 잎을 가졌다고 하여 녹보수라 불립니다.", "밝은 간접광을 좋아하며, 물을 좋아하는 편이므로 흙을 촉촉하게 유지해주세요."),
            WikiData("개운죽", 7, "대나무와 비슷하게 생겼으며, 수경재배로 많이 키우는 행운의 식물입니다.", "직사광선을 피해 밝은 곳에 두세요. 물을 깨끗하게 유지하는 것이 가장 중요합니다."),
            WikiData("호야", 15, "별 모양의 독특한 꽃과 향기가 매력적인 덩굴성 식물입니다.", "밝은 간접광을 좋아하며, 건조하게 관리해야 꽃을 잘 피웁니다."),
            WikiData("틸란드시아 이오난사", 10, "흙 없이 공중에서 자라는 에어플랜트로, 인테리어 소품으로 인기가 많습니다.", "일주일에 2~3회 분무해주거나 한 달에 2~3번 물에 30분 정도 담가주세요. 통풍이 매우 중요합니다."),
            WikiData("크로톤", 8, "빨강, 노랑, 주황 등 화려한 잎 색깔이 매력적인 관엽식물입니다.", "햇빛을 매우 좋아해야 잎 색이 선명해집니다. 흙 표면이 마르면 물을 주세요."),
            WikiData("칼라데아 오르비폴리아", 6, "넓고 둥근 잎에 은색 줄무늬가 있는 아름다운 식물입니다.", "반음지와 높은 습도를 좋아합니다. 흙이 마르지 않게 관리하고 잎에 자주 분무해주세요."),
            WikiData("마란타 레우코네우라", 7, "밤이 되면 잎이 위로 모여 기도하는 손처럼 보인다고 하여 기도 식물이라고도 불립니다.", "반음지와 높은 습도를 선호합니다. 흙을 촉촉하게 유지해주세요."),
            WikiData("드라세나 콤팩타", 15, "성장이 매우 느리고 촘촘하게 잎이 나는 컴팩트한 사이즈의 식물입니다.", "빛이 적은 환경에도 잘 적응합니다. 건조에 강하므로 흙이 완전히 마르면 물을 주세요."),
            WikiData("유칼립투스", 7, "상쾌한 향기가 매력적이며, 실내 장식용으로 인기가 많습니다.", "햇빛과 통풍이 매우 중요합니다. 겉흙이 마르면 물을 듬뿍 주세요."),
            WikiData("올리브 나무", 10, "지중해의 평화로운 분위기를 선사하는 상징적인 식물입니다.", "햇빛을 매우 좋아하며 건조하게 관리해야 합니다. 흙이 완전히 마르면 물을 주세요."),
            WikiData("여인초", 10, "극락조라고도 불리며, 크고 시원한 잎이 이국적인 분위기를 연출합니다.", "밝은 간접광을 좋아하며, 겉흙이 마르면 물을 충분히 줍니다. 잎에 분무해주면 좋습니다."),
            WikiData("박쥐란", 7, "박쥐 날개 모양의 잎이 독특하며, 나무 판에 붙여 벽에 거는 행잉 플랜트로 인기입니다.", "높은 습도를 좋아해 자주 분무해주세요. 뿌리 부분의 솜(물苔)이 마르면 물에 담가 적셔줍니다."),
            WikiData("알로카시아", 8, "코끼리 귀를 닮은 크고 이국적인 잎이 특징적인 식물입니다.", "반양지와 높은 습도를 선호합니다. 과습에 약하므로 겉흙이 마른 것을 확인하고 물을 주세요."),
            WikiData("휘카스 움베르타", 12, "하트 모양의 커다란 잎이 매력적인 고무나무의 일종입니다.", "밝은 간접광에서 잘 자라며, 통풍이 중요합니다. 겉흙이 마르면 물을 듬뿍 주세요."),
            WikiData("필레아 페페로미오이데스", 7, "동전 모양의 둥근 잎이 귀여워 중국 돈 식물이라고도 불립니다.", "밝은 간접광을 좋아하며, 흙이 과습하지 않도록 주의합니다. 겉흙이 마르면 물을 줍니다."),
            WikiData("베고니아 마쿨라타", 8, "물방울 무늬가 있는 독특한 잎과 붉은 뒷면이 아름다운 식물입니다.", "밝은 간접광과 높은 습도를 좋아합니다. 겉흙이 마르면 물을 주세요."),
            WikiData("아디안텀", 4, "하늘하늘하고 여린 잎이 매력적인 고사리과 식물입니다.", "습도가 매우 중요합니다. 흙이 마르지 않게 관리하고, 잎에 자주 분무해주세요. 반음지가 좋습니다."),
            WikiData("무늬 아비스", 9, "새 둥지처럼 잎이 돋아나는 아비스에 아름다운 무늬가 더해진 품종입니다.", "반음지와 높은 습도를 좋아합니다. 흙이 마르지 않도록 관리해주세요."),
            WikiData("립살리스", 14, "숲속 나무에 붙어 자라는 착생 선인장으로, 늘어지는 수형이 매력적입니다.", "밝은 간접광에서 잘 자랍니다. 과습에 주의하며 흙이 완전히 마르면 물을 주세요."),
            WikiData("디시디아", 10, "작고 귀여운 잎이 줄기를 따라 자라며, 행잉 플랜트로 키우기 좋습니다.", "밝은 간접광과 통풍이 잘 되는 곳을 좋아합니다. 물주기는 흙이 마른 것을 확인하고 주세요."),
            WikiData("엔조이 스킨답서스", 7, "기존 스킨답서스에 흰색 무늬가 더해져 화사한 느낌을 주는 품종입니다.", "반음지나 간접광 어디서든 잘 자랍니다. 흙이 마르면 물을 주세요."),
            WikiData("필로덴드론 버킨", 10, "새잎에 아이보리색 줄무늬가 나타나는 것이 특징인 아름다운 필로덴드론입니다.", "밝은 간접광을 좋아하며, 겉흙이 말랐을 때 물을 줍니다. 과습에 주의하세요."),
            WikiData("몬스테라 아단소니", 7, "잎에 구멍이 뚫려 있는 모습이 귀여워 인기가 많은 덩굴성 식물입니다.", "밝은 간접광과 촉촉한 환경을 좋아합니다. 겉흙이 마르면 물을 주세요."),
            WikiData("칼라데아 제브리나", 6, "얼룩말처럼 선명한 줄무늬가 잎에 그려진 칼라데아 품종입니다.", "반음지와 높은 습도를 좋아합니다. 흙이 마르지 않게 관리하고 잎에 자주 분무해주세요."),
            WikiData("피토니아", 5, "선명한 잎맥이 신경망처럼 보인다고 하여 너브 플랜트라고도 불립니다.", "반음지와 높은 습도를 좋아합니다. 물을 매우 좋아하므로 흙이 마르지 않도록 주의해야 합니다."),
            WikiData("콜레우스", 5, "화려하고 다채로운 잎 색깔이 특징으로, 실내 포인트 식물로 좋습니다.", "밝은 빛을 받아야 색이 선명해집니다. 물을 좋아하므로 겉흙이 마르면 듬뿍 주세요."),
            WikiData("게발선인장", 20, "겨울에 아름다운 꽃을 피우는 선인장으로, 게의 발 모양을 닮았습니다.", "밝은 간접광에서 잘 자라며, 꽃이 피는 시기 외에는 건조하게 관리합니다."),
            WikiData("용신목", 30, "팔을 벌리고 있는 사람 모양의 수형이 독특한 선인장입니다.", "햇빛을 매우 좋아합니다. 건조에 강하므로 한 달에 한 번 정도 물을 줍니다."),
            WikiData("만세선인장", 30, "두 팔을 들고 만세를 부르는 듯한 귀여운 모양의 다육식물입니다.", "햇빛을 좋아하며, 과습에 매우 취약합니다. 흙이 완전히 마른 후에 물을 주세요."),
            WikiData("에케베리아", 25, "장미꽃 모양의 잎(로제트)이 특징인 대표적인 다육식물입니다.", "햇빛을 많이 받을수록 예쁘게 자랍니다. 물은 흙이 완전히 마른 후에 주세요."),
            WikiData("하트호야", 20, "사랑스러운 하트 모양 잎 하나로도 잘 자라는 다육질의 식물입니다.", "밝은 간접광을 좋아하며, 건조하게 관리해야 합니다. 과습에 매우 주의하세요."),
            WikiData("수박 페페로미아", 9, "잎의 무늬가 수박 껍질을 닮아 이름 붙여진 귀여운 식물입니다.", "반양지에서 잘 자라며 과습에 약합니다. 겉흙이 마른 것을 확인하고 물을 주세요."),
            WikiData("오렌지 자스민", 7, "자스민과 오렌지 꽃 향기가 나는 흰 꽃을 피우는 향기로운 식물입니다.", "햇빛을 좋아하며, 물을 좋아하므로 겉흙이 마르면 듬뿍 주세요."),
            WikiData("치자나무", 6, "향기로운 흰 꽃을 피우며, 실내에서 향기를 즐기기 좋은 식물입니다.", "햇빛을 좋아하고 물을 말리면 안됩니다. 흙을 촉촉하게 유지해주세요."),
            WikiData("피쉬본 선인장", 15, "생선 뼈 모양의 지그재그 형태 줄기가 특징인 착생 선인장입니다.", "밝은 간접광을 좋아하며, 과습에 주의합니다. 흙이 완전히 마르면 물을 주세요."),
            WikiData("백묘국", 10, "눈이 내린 듯한 은백색의 잎이 아름다운 식물입니다.", "햇빛을 좋아하고 건조한 환경을 선호합니다. 겉흙이 마르면 물을 주세요."),
            WikiData("연필 선인장", 20, "연필처럼 가느다란 줄기가 특징인 유포르비아과 다육식물입니다.", "햇빛을 좋아하며, 건조에 매우 강합니다. 물을 자주 주지 않는 것이 좋습니다."),
            WikiData("트리안", 4, "작고 동그란 잎이 귀여운 덩굴성 식물로, 풍성하게 자랍니다.", "밝은 간접광을 좋아하며, 물을 매우 좋아합니다. 흙이 마르지 않도록 관리해주세요."),
            WikiData("아글라오네마", 10, "화려한 잎 무늬가 특징이며, 빛이 적은 곳에서도 잘 자라는 공기 정화 식물입니다.", "반음지를 선호하며 과습에 약합니다. 겉흙이 마른 것을 확인하고 물을 주세요."),
            WikiData("디펜바키아", 9, "넓고 시원한 잎 무늬가 이국적인 분위기를 만들어주는 식물입니다.", "밝은 간접광을 좋아하며, 흙 표면이 마르면 물을 줍니다. 수액에 독성이 있으니 주의하세요."),
            WikiData("드라세나 마지나타", 14, "용의 피를 닮았다고 하여 '드래곤 트리'라고도 불리는 멋진 수형의 식물입니다.", "밝은 간접광을 좋아하며 건조에 강합니다. 겉흙이 완전히 마르면 물을 주세요."),
            WikiData("싱고니움 핑크", 8, "사랑스러운 핑크빛 잎이 매력적인 싱고니움 품종입니다.", "밝은 간접광에서 색이 더 선명해집니다. 흙을 촉촉하게 유지하는 것이 좋습니다."),
            WikiData("칼라데아 오나타", 6, "섬세한 핑크색 줄무늬가 붓으로 그린 듯 아름다운 식물입니다.", "높은 습도와 반음지를 좋아합니다. 흙이 마르지 않게 관리하고 자주 분무해주세요."),
            WikiData("마란타", 7, "밤이 되면 잎이 모여 기도하는 손처럼 보이는 신비로운 식물입니다.", "반음지와 높은 습도를 선호합니다. 흙을 촉촉하게 유지하고 직사광선은 피해주세요."),
            WikiData("필로덴드론 브라질", 8, "하트 모양 잎에 노란색 무늬가 번진 듯한 모습이 특징입니다.", "반음지에서도 잘 자라며, 겉흙이 마르면 물을 줍니다. 덩굴성으로 자랍니다."),
            WikiData("스킨답서스 픽터스", 10, "은색 점 무늬가 벨벳처럼 보이는 고급스러운 잎을 가진 식물입니다.", "밝은 간접광을 선호하며, 과습에 주의하여 겉흙이 마르면 물을 줍니다."),
            WikiData("알로카시아 폴리", 9, "아프리카 가면을 닮은 독특한 잎 모양과 잎맥이 인상적입니다.", "밝은 간접광과 높은 습도를 좋아합니다. 과습에 약하므로 흙을 건조하게 관리해주세요."),
            WikiData("폴카닷 플랜트", 6, "분홍색, 흰색, 빨간색 등 다양한 점 무늬가 있는 귀여운 식물입니다.", "밝은 간접광을 좋아하며, 흙을 촉촉하게 유지해주세요. 성장이 빠릅니다."),
            WikiData("트라데스칸티아 제브리나", 7, "얼룩말 무늬와 보라색 뒷면이 매력적인 덩굴성 식물입니다.", "햇빛을 보면 색이 더 선명해집니다. 흙이 마르면 물을 주고, 쉽게 번식합니다."),
            WikiData("크리스마스 캑터스", 20, "가재발 선인장이라고도 불리며, 크리스마스 즈음에 아름다운 꽃을 피웁니다.", "밝은 간접광에서 잘 자라며, 꽃이 피는 시기 외에는 건조하게 관리합니다."),
            WikiData("렉스 베고니아", 8, "화려하고 독특한 모양, 색상, 질감의 잎을 가진 관상용 베고니아입니다.", "밝은 간접광과 높은 습도를 선호합니다. 과습에 매우 약하니 주의하세요."),
            WikiData("클리비아 (군자란)", 15, "겨울과 봄 사이 주황색의 아름다운 꽃을 피우는 난초과 식물입니다.", "밝은 간접광을 좋아하며, 건조에 강합니다. 꽃을 보려면 서늘한 겨울을 나야 합니다."),
            WikiData("자스민", 7, "달콤하고 강한 향기를 가진 흰 꽃을 피우는 덩굴성 식물입니다.", "햇빛이 잘 드는 곳을 좋아하며, 겉흙이 마르면 물을 듬뿍 줍니다."),
            WikiData("세덤 (돌나물과)", 20, "종류가 매우 다양하며, 건조한 환경에 잘 적응하는 다육식물입니다.", "햇빛을 좋아하며, 과습에 매우 취약합니다. 배수가 잘 되는 흙에 심어주세요."),
            WikiData("하월시아", 25, "투명한 잎 창(window)이 특징인 작고 신비로운 다육식물입니다.", "강한 직사광선보다는 밝은 간접광을 선호합니다. 물주기는 흙이 완전히 마른 후에 합니다."),
            WikiData("녹영 (콩란)", 15, "녹색 구슬 같은 잎이 아래로 늘어지며 자라 'String of Pearls'라고 불립니다.", "밝은 간접광을 좋아하며, 과습에 매우 약합니다. 흙이 마르면 물을 주세요."),
            WikiData("러브체인", 12, "하트 모양의 잎이 체인처럼 길게 늘어지는 사랑스러운 식물입니다.", "밝은 간접광을 선호하며, 건조에 강한 편입니다. 흙이 마르면 물을 줍니다."),
            WikiData("리톱스", 40, "살아있는 돌이라고 불리며, 독특한 모양과 탈피 과정이 신기한 다육식물입니다.", "햇빛을 매우 좋아하며, 물을 거의 주지 않아도 됩니다. 특히 여름과 겨울에는 단수하세요."),
            WikiData("백도선 (토끼 귀 선인장)", 30, "토끼 귀를 닮은 모양에 흰 솜털 같은 가시가 있는 귀여운 선인장입니다.", "햇빛을 매우 좋아합니다. 물은 한 달에 한 번 정도 듬뿍 주세요."),
            WikiData("아스파라거스 펀", 5, "고사리와 비슷하지만 다른 종으로, 부드럽고 섬세한 잎이 매력적입니다.", "반양지와 촉촉한 흙을 좋아합니다. 잎에 자주 분무해주면 건강하게 자랍니다."),
            WikiData("아펠란드라", 8, "선명한 노란색 포와 얼룩말 같은 잎맥이 인상적인 식물입니다.", "밝은 간접광과 높은 습도를 좋아합니다. 흙을 촉촉하게 유지해주세요."),
            WikiData("칼라데아 진저", 7, "생강과 비슷한 꽃을 피우며 아름다운 잎을 가진 칼라데아 품종입니다.", "반음지와 높은 습도를 좋아합니다. 흙을 촉촉하게 유지하고 잎에 자주 분무해주세요."),
            WikiData("휘토니아", 5, "잎맥의 색상이 매우 화려하여 '모자이크 식물'이라고도 불립니다.", "높은 습도와 낮은 광량을 선호하여 테라리움에 적합합니다. 흙이 마르지 않게 관리하세요."),
            WikiData("네마탄서스", 10, "금붕어를 닮은 주황색 꽃이 피어 '금붕어초'라고도 불립니다.", "밝은 간접광을 좋아하며, 겉흙이 마르면 물을 줍니다. 행잉 화분에 잘 어울립니다."),
            WikiData("익소라", 8, "작은 꽃들이 공처럼 뭉쳐서 피는 모습이 아름다운 열대 식물입니다.", "햇빛을 매우 좋아하며, 물을 좋아합니다. 따뜻한 환경을 유지해주세요.")
        )

        // 일괄 쓰기 (Batch Write) 수행
        // Firestore Batch는 한 번에 500개까지 가능하므로 100개는 한 번에 처리가능
        plants.forEach { plant ->
            // ID 자동 생성을 위해 document() 사용
            val docRef = collection.document()
            batch.set(docRef, plant)
        }

        val future = batch.commit()

        // 완료 대기
        future.get(30, TimeUnit.SECONDS)

        val msg = "${plants.size}개의 식물 위키 데이터가 Firestore에 성공적으로 저장되었습니다."
        log.info(msg)
        return ResponseEntity.ok(msg)
    }

    @GetMapping("/users")
    fun migrateUserData(): ResponseEntity<String> {
        // 제공해주신 CSV 데이터를 Kotlin 객체로 변환했습니다.
        val users = listOf(
            UserData(4490627148, "LoveLoveFall", "cE35GUDMTj6QWxEFT9DGfv:APA91bHb9RtdO9PEHXsF3XIAtIgzKSiZO1LAWkMZO9oIhh2g1bIkUeE92tlEtBejfc8OtW9S098VLcAVQkJm3OOBWDZb8h0lgFpz1af0rZ0Wo9aet8_pDmA"),
            UserData(4499266908, "heeblings테스터", "f_3AcQ12TAavvAwKh_xyv6:APA91bHw5tdBt6jayYhp79RWQnzKXBxKajBbs2RWyZMnK8zxzzLRi-oMfMABTPCZENGDAiC9tHUWm-TrNUk-1_AEPUnWSgm1YQn4KTBCLuruiXYOuVog1WA"),
            UserData(4537595832, "안유", "f3DNuGSnSTqtz9r3ii1FNw:APA91bFY3bvK0p1fJvDNCofpuTRHXJno8X0UnDlTXw-hUDy-BGxGE3v6fHBD-HT-yge0fq4ozC7o7tozc4b_5Tc1qcek0G3P-nWgNuzCBwWA0Ng0p3_WW-8"),
            UserData(4537603240, "박요한", "dTA5bFU8QdOgyl2dglrRG4:APA91bGngtUX7asQsvNGL3dAPGEI_BXEA68KLfsxrhmnAIsdBa_nlld0bkeJtvhJq9CZ3qqvg2dh4iOs6cozJ3DEuhl0q-E7-pbulrQdqNfWDbyzuFuX2ng"),
            UserData(4537622772, "유진 하일 엄마 최경미", "dAMsvH6zQKObf8vf7TDgKi:APA91bHTn7bYruLgEYuSfkC6EAxr9WkrpGuENS6t2jrw5phUwotrN5pKxHHYPEhMWq1cpndI3Ddt9DGJ-B2fmKye2RZZOAAeaMF6jczsfeKjOq0na2Imy2E"),
            UserData(4537625983, "허남회", "dgcMxSZ6TlGlqEbstkMKve:APA91bGW0pSM-QvoXB4BmerHIDG0UjfYjwDhyEuyX_-wiEy_jqvGiEfIzc6f7FrahYZmfoXaGOjWRlTZiZec7FMo4gGvHJy3znzMkVNGFct0V0VTG8z3O2U"),
            UserData(4537665399, ".....", "c53cfjfcRwKT9BJxju9-6y:APA91bHjEu8JU5W29y42oe5zhVKPs7srlYaXK3F6hHDEFxKk84Ylos-e0WzRGh11Z0WwDQG1yFlYfLuCqaQd4IuUopd3sEqg5z059QZa0Ej93lTLPRwupZ8"),
            UserData(4537726305, "김혜란", "elNhbdwZQ-isLUVvLfD52Y:APA91bGI9E_TaLvYp_iXhbAIf0YEP-l2RZQi4RJsqvRUAbYGe9k0M-ePzyiINI5Iqor3E3k-4b8n-YyqoocQwJc8CRoJ2P7g9484qqrewywlGJI9B8x4xlw"),
            UserData(4537729460, "김성찬", "ejkf2At1Q_WeQp4hIIjpzo:APA91bGAeoYMPXVBDCqpKrhumY4D2F8u8BecIJFSeHN8s8mHQP7vqXa6KBIwDUxwSqabMHBdahmnYHyKa7_aGigoFTuGGZoLkgWVcBsX0HTMjMx81w-Imww"),
            UserData(4537751806, "현재", "fGKkmSrGSlCJjAtGb5aOLG:APA91bE13JpBuX9pxutNjDJXS4VN3y5kmb3kqk5kFKyJqEJ5-QjieZSRXz2uo9iNzlUXQcjAvNmFu-xyA9LUBs9llTGL8Y8paSujgk-Eh6RAJZM5S6uN2x4"),
            UserData(4538157332, "최형기", "eML8sjJ6Qh-v1S5Ien1_Xx:APA91bFeggEo6LVpyxd24hfooTceVu0Cc6i0kp5aMQRBa6eyVcqfhuB1w64tpY5oxi6omiQCykzQFaVwuUUph7lUldhmI_Mp5FmdINdC8DhO8sKIU3WUQEA"),
            UserData(4538259429, "Grace은혜", "eCb-1UwDRKacRMAIwOXahT:APA91bEPzioQDG_vmJ12jDkhVnPt5sBANQg-95UDpj--lnsyjK0hQH6q9rUl59aeLD_Fhp2KPsg9_uO2gd-naNUBBXXis8zBxO76x1Xu6JubLsGtLfKjW3o"),
            UserData(4538272846, "이금옥", "fm2i-zEVQyeJz-NqEXqbTO:APA91bHweCu_AcfFO-EZ0nBAeEWgXc04PSOE_aMegIHnzZPkqvql6VYt4_WwK0Eve6i42amyYC1WzSnYF13G0zVx7e0TV1L5N5S-0ikpC0uwujnUjAk8WKc"),
            UserData(4544524900, "정희정", "cS5iIfGZR7GedwZr38Y4Fv:APA91bEn4gEFdLn3Yurk9lK5S3JbTJTKue2ifJLeri-jNUrSLY3SyVfXgZVw1WmIwP9CMj2PEmGSRM_2wcNZw1_40m33bEb5_LfBy6ZUViEq7V6OATUsA04")
        )

        var successCount = 0
        users.forEach { user ->
            try {
                // FirestoreService의 기존 로직을 재사용 (users 컬렉션에 저장)
                firestoreService.saveOrUpdateUser(user.kakaoId, user.nickname, user.fcmToken)
                successCount++
            } catch (e: Exception) {
                log.error("Failed to migrate user: ${user.nickname} (${user.kakaoId})", e)
            }
        }

        val msg = "총 ${users.size}명 중 ${successCount}명의 사용자 데이터가 Firestore로 마이그레이션 되었습니다."
        log.info(msg)
        return ResponseEntity.ok(msg)
    }

    @GetMapping("/plants")
    fun migratePlantData(): ResponseEntity<String> {
        // 1. User ID -> Kakao ID 매핑 테이블 (이전 User 데이터 기반)
        val userIdToKakaoId = mapOf(
            3L to "4490627148",
            7L to "4477998752",
            8L to "4499266908",
            9L to "4537595832",
            12L to "4537625983",
            14L to "4537726305",
            15L to "4537729460",
            17L to "4538157332"
            // 필요한 경우 다른 ID도 추가
        )

        // 2. CSV 데이터 리스트
        val plants = listOf(
            PlantCsvData(3, "오호라", null, "COMPLETE", "스투키", "2025-10-01", "2025-11-19", null, 25, "2025-12-14", "산세베리아의 한 종류로, 공기 정화 능력이 뛰어나고 관리가 쉽습니다.", "건조에 매우 강하므로 물을 자주 주지 마세요. 한 달에 한 번 정도가 적당합니다.", "2025-10-12 19:25:51", "2025-11-19 17:45:12"),
            PlantCsvData(8, "콩콩이", "63fa270a-6c05-49a9-b8c9-25e4dd3fb3b8.jpg", "COMPLETE", "고무나무", "2025-10-18", null, "2025-10-18", 12, null, "윤기나는 넓은 잎과 미세먼지 제거 효과로 사랑받는 식물입니다.", "밝은 간접광을 좋아하며, 흙 표면이 말랐을 때 물을 충분히 줍니다.", "2025-10-18 10:43:43", "2025-10-18 10:44:13"),
            PlantCsvData(8, "쭉쭉이", "ec217c76-ae88-4e06-97e5-1c554d8b58ee.jpg", "COMPLETE", "보스턴고사리", "2025-10-15", "2025-10-15", null, 5, "2025-10-20", "풍성한 잎이 매력적이며, 가습 효과와 포름알데히드 제거 능력이 뛰어납니다.", "높은 습도를 좋아하므로 흙을 항상 촉촉하게 유지하고 잎에 자주 분무해주세요. 반음지에서 키우세요.", "2025-10-18 10:44:55", "2025-10-18 10:45:34"),
            PlantCsvData(3, "스투키", null, "COMPLETE", "스투키", "2025-10-25", "2025-11-19", null, 25, "2025-12-14", "산세베리아의 한 종류로, 공기 정화 능력이 뛰어나고 관리가 쉽습니다.", "건조에 매우 강하므로 물을 자주 주지 마세요. 한 달에 한 번 정도가 적당합니다.", "2025-10-29 13:37:48", "2025-11-19 17:45:11"),
            PlantCsvData(3, "행복이", null, "COMPLETE", "해피트리 (파키라)", "2025-11-01", "2025-11-19", null, 15, "2025-12-04", "행운을 가져다준다고 알려져 있으며, 굵은 줄기와 풍성한 잎이 특징입니다.", "반양지에서 잘 자라며, 건조에 강한 편입니다. 흙이 마르면 물을 줍니다.", "2025-11-03 15:31:43", "2025-11-19 17:45:10"),
            PlantCsvData(12, "나미야", null, "COMPLETE", "여인초", "2025-11-10", "2025-11-10", null, 10, "2025-11-20", "극락조라고도 불리며, 크고 시원한 잎이 이국적인 분위기를 연출합니다.", "밝은 간접광을 좋아하며, 겉흙이 마르면 물을 충분히 줍니다. 잎에 분무해주면 좋습니다.", "2025-11-10 12:20:25", "2025-11-10 12:20:25"),
            PlantCsvData(15, "봉송이", null, "COMPLETE", "고무나무", "2025-11-01", "2025-11-08", null, 12, "2025-11-20", "윤기나는 넓은 잎과 미세먼지 제거 효과로 사랑받는 식물입니다.", "밝은 간접광을 좋아하며, 흙 표면이 말랐을 때 물을 충분히 줍니다.", "2025-11-10 13:11:40", "2025-11-10 13:11:40"),
            PlantCsvData(14, null, "8e9bcdc0-01c7-40f8-a9d2-db5de804ce74.jpg", "COMPLETE", "로즈마리", "2025-11-10", "2025-11-15", null, 6, "2025-11-21", "상쾌한 향기가 매력적인 허브로, 요리에도 활용할 수 있습니다.", "햇빛이 잘 들고 통풍이 잘 되는 곳을 좋아합니다. 흙이 말랐을 때 물을 주세요.", "2025-11-10 13:12:05", "2025-11-15 21:47:45"),
            PlantCsvData(9, "버들이", null, "COMPLETE", "녹영 (콩란)", "2025-11-10", "2025-11-14", null, 15, "2025-11-29", "녹색 구슬 같은 잎이 아래로 늘어지며 자라 <String of Pearls>라고 불립니다.", "밝은 간접광을 좋아하며, 과습에 매우 약합니다. 흙이 마르면 물을 주세요.", "2025-11-10 16:03:47", "2025-11-14 16:21:43"),
            PlantCsvData(14, null, null, "COMPLETE", "고무나무", "2025-11-04", "2025-11-18", null, 12, "2025-11-30", "윤기나는 넓은 잎과 미세먼지 제거 효과로 사랑받는 식물입니다.", "밝은 간접광을 좋아하며, 흙 표면이 말랐을 때 물을 충분히 줍니다.", "2025-11-10 18:37:13", "2025-11-18 21:38:56"),
            PlantCsvData(9, null, null, "COMPLETE", "관음죽", "2025-11-11", "2025-11-14", null, 9, "2025-11-23", "동양적인 매력이 있고 병충해에 강한 공기 정화 식물입니다.", "반음지나 간접광에서 잘 자라며, 물을 좋아하는 편이므로 흙을 촉촉하게 유지해주세요.", "2025-11-11 14:26:03", "2025-11-14 16:21:43"),
            PlantCsvData(14, null, null, "COMPLETE", "관음죽", "2025-11-11", "2025-11-15", null, 9, "2025-11-24", "동양적인 매력이 있고 병충해에 강한 공기 정화 식물입니다.", "반음지나 간접광에서 잘 자라며, 물을 좋아하는 편이므로 흙을 촉촉하게 유지해주세요.", "2025-11-11 14:30:41", "2025-11-15 21:47:47"),
            PlantCsvData(17, "스튜키", null, "COMPLETE", "스투키", "2025-11-01", "2025-11-11", null, 25, "2025-12-06", "산세베리아의 한 종류로, 공기 정화 능력이 뛰어나고 관리가 쉽습니다.", "건조에 매우 강하므로 물을 자주 주지 마세요. 한 달에 한 번 정도가 적당합니다.", "2025-11-11 23:47:16", "2025-11-11 23:49:05"),
            PlantCsvData(7, "쭉쭉이", "0c3b9915-8dac-4fee-a785-437dc21ac9a8.jpg", "COMPLETE", "알로카시아", "2025-11-01", "2025-11-16", null, 8, "2025-11-24", "코끼리 귀를 닮은 크고 이국적인 잎이 특징적인 식물입니다.", "반양지와 높은 습도를 선호합니다. 과습에 약하므로 겉흙이 마른 것을 확인하고 물을 주세요.", "2025-11-12 12:19:59", "2025-11-16 17:22:59"),
            PlantCsvData(9, "도", null, "COMPLETE", "금전수 (돈나무)", "2025-11-12", "2025-11-14", null, 15, "2025-11-29", "돈과 행운을 불러온다고 알려져 개업 선물로 인기가 많습니다.", "반음지에서도 잘 자라며 건조에 강합니다. 흙이 완전히 말랐을 때 물을 주는 것이 좋습니다.", "2025-11-12 22:53:25", "2025-11-14 16:21:44"),
            PlantCsvData(7, "스툭이", "a4899ff3-c265-4311-91ab-efe53366f21d.jpg", "COMPLETE", "스투키", "2025-11-01", "2025-11-18", null, 25, "2025-12-13", "산세베리아의 한 종류로, 공기 정화 능력이 뛰어나고 관리가 쉽습니다.", "건조에 매우 강하므로 물을 자주 주지 마세요. 한 달에 한 번 정도가 적당합니다.", "2025-11-13 00:46:27", "2025-11-18 15:31:19"),
            PlantCsvData(9, "레", null, "COMPLETE", "녹보수", "2025-11-13", "2025-11-14", null, 10, "2025-11-24", "보석처럼 반짝이는 녹색 잎을 가졌다고 하여 녹보수라 불립니다.", "밝은 간접광을 좋아하며, 물을 좋아하는 편이므로 흙을 촉촉하게 유지해주세요.", "2025-11-13 18:11:18", "2025-11-14 16:21:46"),
            PlantCsvData(9, "미", null, "COMPLETE", "스킨답서스", "2025-11-14", "2025-11-14", null, 7, "2025-11-21", "생명력이 강해 초보자도 쉽게 키울 수 있는 대표적인 실내 식물입니다.", "반음지나 간접광 어디서든 잘 자랍니다. 흙이 마르면 물을 주세요. 수경재배도 가능합니다.", "2025-11-14 06:39:52", "2025-11-14 06:39:52"),
            PlantCsvData(9, "파", null, "COMPLETE", "무늬 아비스", "2025-11-14", "2025-11-14", null, 9, "2025-11-23", "새 둥지처럼 잎이 돋아나는 아비스에 아름다운 무늬가 더해진 품종입니다.", "반음지와 높은 습도를 좋아합니다. 흙이 마르지 않도록 관리해주세요.", "2025-11-14 12:41:21", "2025-11-14 12:41:21"),
            PlantCsvData(14, null, null, "COMPLETE", "보스턴고사리", "2024-10-16", "2025-11-19", null, 5, "2025-11-24", "풍성한 잎이 매력적이며, 가습 효과와 포름알데히드 제거 능력이 뛰어납니다.", "높은 습도를 좋아하므로 흙을 항상 촉촉하게 유지하고 잎에 자주 분무해주세요. 반음지에서 키우세요.", "2025-11-16 14:29:57", "2025-11-19 09:00:24"),
            PlantCsvData(9, "솔", null, "COMPLETE", "호야", "2025-11-17", "2025-11-17", null, 15, "2025-12-02", "별 모양의 독특한 꽃과 향기가 매력적인 덩굴성 식물입니다.", "밝은 간접광을 좋아하며, 건조하게 관리해야 꽃을 잘 피웁니다.", "2025-11-17 09:49:49", "2025-11-17 09:49:49"),
            PlantCsvData(14, null, null, "COMPLETE", "스킨답서스", "2025-04-14", "2025-11-18", null, 7, "2025-11-25", "생명력이 강해 초보자도 쉽게 키울 수 있는 대표적인 실내 식물입니다.", "반음지나 간접광 어디서든 잘 자랍니다. 흙이 마르면 물을 주세요. 수경재배도 가능합니다.", "2025-11-18 21:42:00", "2025-11-18 21:42:05"),
            PlantCsvData(3, "ㅎㅎ", null, "COMPLETE", "디시디아", "2025-11-01", "2025-11-19", null, 10, "2025-11-29", "작고 귀여운 잎이 줄기를 따라 자라며, 행잉 플랜트로 키우기 좋습니다.", "밝은 간접광과 통풍이 잘 되는 곳을 좋아합니다. 물주기는 흙이 마른 것을 확인하고 주세요.", "2025-11-19 17:45:45", "2025-11-19 17:45:46")
        )

        var successCount = 0
        var failCount = 0
        val formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formatterDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        plants.forEach { plant ->
            try {
                val kakaoId = userIdToKakaoId[plant.userId]
                if (kakaoId == null) {
                    log.warn("Kakao ID not found for user ID: ${plant.userId}. Skipping plant '${plant.nickname}'.")
                    failCount++
                    return@forEach
                }

                // 날짜 변환 유틸
                fun parseDate(dateStr: String?): Date? {
                    if (dateStr.isNullOrBlank()) return null
                    return try {
                        Date.from(LocalDate.parse(dateStr, formatterDate).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant())
                    } catch (e: Exception) { null }
                }

                fun parseDateTime(dateStr: String?): Date {
                    if (dateStr.isNullOrBlank()) return Date()
                    return try {
                        Date.from(java.time.LocalDateTime.parse(dateStr, formatterDateTime).atZone(ZoneId.of("Asia/Seoul")).toInstant())
                    } catch (e: Exception) { Date() }
                }

                val startDate = parseDate(plant.startDate)
                val lastWateredDate = parseDate(plant.lastWateredDate)
                val lastRepottedDate = parseDate(plant.lastRepottedDate)
                val nextWateringDate = parseDate(plant.nextWateringDate)
                val nextWateringDateMillis = nextWateringDate?.time
                val createdAt = parseDateTime(plant.createdAt)
                val updatedAt = parseDateTime(plant.updatedAt)

                // Firestore 저장용 Map 생성
                val plantMap = mutableMapOf<String, Any?>()
                plantMap["nickname"] = plant.nickname
                plantMap["imageUrl"] = plant.imageUrl
                plantMap["imageStatus"] = plant.imageStatus
                plantMap["plantType"] = plant.plantType
                plantMap["startDate"] = startDate
                plantMap["lastWateredDate"] = lastWateredDate
                plantMap["lastRepottedDate"] = lastRepottedDate
                plantMap["wateringCycleDays"] = plant.wateringCycleDays
                plantMap["nextWateringDate"] = nextWateringDate
                plantMap["nextWateringDateMillis"] = nextWateringDateMillis
                plantMap["description"] = plant.description
                plantMap["careInfo"] = plant.careInfo
                plantMap["createdAt"] = createdAt
                plantMap["updatedAt"] = updatedAt
                plantMap["userId"] = kakaoId // 쿼리 편의를 위해 추가

                // FirestoreService 재사용 (식물 저장)
                firestoreService.savePlant(kakaoId, plantMap)
                successCount++

            } catch (e: Exception) {
                log.error("Failed to migrate plant for user ${plant.userId}", e)
                failCount++
            }
        }

        val msg = "식물 데이터 마이그레이션 완료: 성공 $successCount 건, 실패/건너뜀 $failCount 건"
        log.info(msg)
        return ResponseEntity.ok(msg)
    }

    @GetMapping("/push-messages")
    fun migratePushMessages(): ResponseEntity<String> {
        // 1. User ID -> Kakao ID 매핑 (이전 데이터 기반)
        val userIdToKakaoId = mapOf(
            3L to "4490627148",
            7L to "4477998752",
            8L to "4499266908",
            9L to "4537595832",
            12L to "4537625983",
            14L to "4537726305",
            15L to "4537729460",
            17L to "4538157332"
            // 필요한 경우 다른 ID도 추가
        )

        // 2. CSV 데이터 리스트
        val messages = listOf(
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-14 09:00:00"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-16 09:00:00"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-17 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-19 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-20 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-20 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-21 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-21 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-22 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-22 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-23 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-23 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-24 09:00:02"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-24 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-25 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-25 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-26 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-26 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-27 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-27 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-28 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-28 09:00:02"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-29 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-10-29 09:00:02"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-30 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-10-31 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-01 09:00:02"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-02 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-03 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-04 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-05 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-11-05 09:00:02"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-06 09:00:02"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-07 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-11-07 09:00:03"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-08 09:00:02"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-11-08 09:00:03"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-09 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-10 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-11 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-11-11 09:00:02"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-12 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-13 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-11-13 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-14 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-11-14 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-15 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-11-15 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-16 09:00:01"),
            PushMessageCsvData(7, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 1, "2025-11-16 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-17 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-18 09:00:01"),
            PushMessageCsvData(8, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-19 09:00:01"),
            PushMessageCsvData(14, "🪴 식물 물주기 알림", "오늘 물이 필요한 친구들이 있어요. 잊지 말고 상태를 확인해주세요!", 0, "2025-11-19 09:00:01")
        )

        var successCount = 0
        var failCount = 0
        val formatterDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // Firestore Batch (최대 500개)
        val batch = firestore.batch()

        messages.forEach { msg ->
            try {
                val kakaoId = userIdToKakaoId[msg.userId]
                if (kakaoId == null) {
                    log.warn("Kakao ID not found for user ID: ${msg.userId}. Skipping message.")
                    failCount++
                    return@forEach
                }

                val createdDateTime = try {
                    java.time.LocalDateTime.parse(msg.createdAt, formatterDateTime)
                } catch (e: Exception) {
                    java.time.LocalDateTime.now()
                }
                val createdAtDate = Date.from(createdDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant())

                val messageDoc = firestore.collection("users").document(kakaoId)
                    .collection("pushMessages").document()

                val messageMap = mapOf(
                    "title" to msg.title,
                    "body" to msg.body,
                    "isRead" to (msg.isRead == 1),
                    "createdAt" to createdAtDate
                )

                batch.set(messageDoc, messageMap)
                successCount++

            } catch (e: Exception) {
                log.error("Failed to migrate message for user ${msg.userId}", e)
                failCount++
            }
        }

        // 일괄 커밋
        val future = batch.commit()
        future.get(30, TimeUnit.SECONDS)

        val resultMsg = "푸시 메시지 마이그레이션 완료: 성공 $successCount 건, 실패/건너뜀 $failCount 건"
        log.info(resultMsg)
        return ResponseEntity.ok(resultMsg)
    }
}