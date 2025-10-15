#!/bin/bash

# 이 스크립트는 애플리케이션이 배포되기 직전(predeploy)에 실행됩니다.
echo "Setting JVM memory options"

# JAVA_TOOL_OPTIONS 환경 변수에 메모리 설정(-Xms, -Xmx)과 시간대 설정을 함께 추가합니다.
# EB Java 플랫폼은 이 환경 변수를 읽어 애플리케이션 실행 시 적용합니다.
export JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m -Duser.timezone=Asia/Seoul"

# 스크립트가 실행 가능하도록 권한을 부여합니다.
chmod +x /var/app/staging/.platform/hooks/predeploy/02_jvm_options.sh