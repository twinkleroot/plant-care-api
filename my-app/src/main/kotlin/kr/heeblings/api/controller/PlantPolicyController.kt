package kr.heeblings.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/plant-app")
class PlantPolicyController {

    @GetMapping("/privacy")
    fun privacyPolicy(): String {
        // "plant_privacy"라는 이름의 뷰(templates/plant_privacy.html)를 반환합니다.
        return "plant_privacy"
    }

    @GetMapping("/data-deletion")
    fun dataDeletionPolicy(): String {
        // "plant_data_deletion"라는 이름의 뷰(templates/plant_data_deletion.html)를 반환합니다.
        return "plant_data_deletion"
    }
}
