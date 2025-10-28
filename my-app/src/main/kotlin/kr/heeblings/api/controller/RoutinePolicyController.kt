package kr.heeblings.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/routine-manager-app")
class RoutinePolicyController {

    @GetMapping("/account-deletion")
    fun deleteAccountInfo(): String {
        // "account-deletion"라는 이름의 뷰(templates/routine_account_deletion.html)를 반환합니다.
        return "routine_account_deletion"
    }

    @GetMapping("/privacy")
    fun privacyPolicy(): String {
        // "routine_privacy"라는 이름의 뷰(templates/routine_privacy.html)를 반환합니다.
        return "routine_privacy"
    }
}