package kr.heeblings.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/parent-helper-app")
class ParentHelperPolicyController {

    @GetMapping("/account-deletion")
    fun deleteAccountInfo(): String {
        // "account-deletion"라는 이름의 뷰(templates/parent_helper_account_deletion.html)를 반환합니다.
        return "parent_helper_account_deletion"
    }

    @GetMapping("/privacy")
    fun privacyPolicy(): String {
        // "parent_helper_privacy"라는 이름의 뷰(templates/parent_helper_privacy.html)를 반환합니다.
        return "parent_helper_privacy"
    }
}