package kr.heeblings.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TestAppController {

    @GetMapping("/literacy-test-app/privacy")
    fun literacyPrivacyPolicy(): String {
        // "literacy_test_privacy"라는 이름의 뷰(templates/literacy_test_privacy.html)를 반환합니다.
        return "literacy_test_privacy"
    }

    @GetMapping("/stress-test-app/privacy")
    fun stressPrivacyPolicy(): String {
        // "stress_test_privacy"라는 이름의 뷰(templates/stress_test_privacy.html)를 반환합니다.
        return "stress_test_privacy"
    }

    @GetMapping("/general-knowledge-test-app/privacy")
    fun generalKnowledgePrivacyPolicy(): String {
        // "general_knowledge_test_privacy"라는 이름의 뷰(templates/general_knowledge_test_privacy.html)를 반환합니다.
        return "general_knowledge_test_privacy"
    }
}