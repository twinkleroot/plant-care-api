// 전체 프로젝트에서 사용할 플러그인들의 버전을 여기서 중앙 관리합니다.
// apply false는 "지금 바로 적용하지 말고, 하위 모듈이 필요할 때 쓸 수 있도록 준비만 해둬라" 라는 의미입니다.
plugins {
	id("org.springframework.boot") version "3.2.5" apply false
	id("io.spring.dependency-management") version "1.1.4" apply false
	kotlin("jvm") version "1.9.23" apply false
	kotlin("plugin.spring") version "1.9.23" apply false
	kotlin("plugin.jpa") version "1.9.23" apply false
}
