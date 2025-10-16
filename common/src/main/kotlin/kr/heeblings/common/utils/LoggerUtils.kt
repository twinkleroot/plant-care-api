package kr.heeblings.common.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// 어떤 클래스든 이 확장 함수를 import하면 'log'라는 프로퍼티를 바로 사용할 수 있습니다.
// val log = LoggerFactory.getLogger(this::class.java) 코드를 자동으로 생성해주는 것과 같습니다.
val <T : Any> T.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
