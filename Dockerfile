# ================================================================
# sparta-cafe Dockerfile
# 빌드 흐름:
#   1. ./gradlew bootJar 로 jar 생성 (쉘 스크립트에서 먼저 실행)
#   2. docker compose up --build 시 이 파일을 읽어서 이미지 생성
# ================================================================

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# bootJar 로 생성된 jar 파일 복사
# build/libs/ 아래에 *-SNAPSHOT.jar 형태로 생성됨
COPY build/libs/*.jar app.jar

# 타임존 설정 (application.yml 의 Asia/Seoul 과 일치)
ENV TZ=Asia/Seoul

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]