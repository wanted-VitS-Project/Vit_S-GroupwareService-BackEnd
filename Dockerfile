# ===== build: Gradle bootJar (Spring Boot 3.5) =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# 의존성 레이어 캐시: 빌드 스크립트/래퍼 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
# 소스 복사 후 실행 가능 jar 생성 (테스트는 CI 가 별도로 — 이미지 빌드는 jar 만)
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# ===== run: JRE 17 =====
FROM eclipse-temurin:17-jre
WORKDIR /app
# 컨테이너/JVM 타임존 KST 고정 (기본 UTC → created_at·로그 시각 KST 통일)
ENV TZ=Asia/Seoul
COPY --from=build /app/build/libs/*.jar app.jar
# 비루트 사용자로 실행 — 컨테이너 침해 시 권한 범위 축소
RUN useradd -r -u 1001 appuser && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
# -Duser.timezone: OS tzdata 유무와 무관하게 JVM 기본 타임존을 KST 로 못박음
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
