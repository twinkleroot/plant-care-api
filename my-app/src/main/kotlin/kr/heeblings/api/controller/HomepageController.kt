package kr.heeblings.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping
class HomepageController {
    @GetMapping("/")
    fun policyHub(): String {
        // "policy_hub"라는 이름의 뷰(templates/policy_hub.html)를 반환합니다.
        return "policy_hub"
    }
}