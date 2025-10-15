#!/bin/bash

# 이 스크립트는 애플리케이션이 배포되기 직전(predeploy)에 실행됩니다.
echo "Setting server timezone to Asia/Seoul"
sudo timedatectl set-timezone Asia/Seoul
