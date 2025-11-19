package kr.heeblings.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/church")
class ChurchSchoolController {
    @GetMapping("/bible/preschool")
    fun preschool(): String {
        return "church_school_bible_preschool"
    }

    @GetMapping("/bible/elementary-school")
    fun elementarySchool(): String {
        return "church_school_bible_elementary_school"
    }
}