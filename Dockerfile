# ================================
# 1. Builder Stage
# ================================
FROM gradle:8.10-jdk17-alpine AS builder
WORKDIR /app

# Gradle 캐시 최적화
COPY build.gradle gradle.properties ./ 
RUN gradle dependencies --no-daemon || true

# 나머지 소스 복사
COPY . .

# 빌드 (테스트 스킵)
RUN gradle clean build -x test --no-daemon

# ================================
# 2. Runtime Stage
# ================================
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
