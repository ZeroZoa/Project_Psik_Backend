# ── 1단계: 빌드 ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Gradle Wrapper + 의존성 캐시 레이어
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# 소스 빌드 (테스트 제외)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 레이어드 JAR 분리 (Docker 레이어 캐시 효율화)
RUN mkdir -p build/extracted && \
    java -Djarmode=layertools \
    -jar build/libs/psik-0.0.1-SNAPSHOT.jar \
    extract --destination build/extracted

# ── 2단계: 실행 ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 보안: root 대신 전용 유저 실행
RUN addgroup -S psik && adduser -S psik -G psik

# 레이어드 JAR 복사 (변경 빈도 낮은 순 → 높은 순으로 캐시 최적화)
COPY --from=builder --chown=psik:psik /workspace/build/extracted/dependencies/ ./
COPY --from=builder --chown=psik:psik /workspace/build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=psik:psik /workspace/build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=psik:psik /workspace/build/extracted/application/ ./

USER psik

# Cloud Run 기본 포트
EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+TieredCompilation", \
    "-Dspring.profiles.active=prod", \
    "-Dserver.port=8080", \
    "org.springframework.boot.loader.launch.JarLauncher"]