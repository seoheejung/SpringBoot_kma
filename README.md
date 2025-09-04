# 🚀 InfluxDB & Spring Boot 시계열 데이터 처리 프로젝트

## 1. 프로젝트 목표
Spring Boot 3.x와 InfluxDB를 활용해 센서 데이터(예: 온도)를 시계열로 저장하고, REST API로 조회할 수 있는 서비스를 구현합니다.  
Repository 계층은 **Kotlin**을 도입하여 Java + Kotlin 혼합 환경을 실험합니다.  
개발 및 배포는 **Docker (Java 17 + Gradle 8.10)** 환경에서 진행합니다.

---

## 2. 기술 스택
- **Backend:** Spring Boot 3.3.x, Java 17, Kotlin
- **Database:** InfluxDB 2.x
- **Build Tool:** Gradle 8.10
- **Container:** Docker (멀티스테이지 빌드)
- **Monitoring (선택):** Grafana, Spring Boot Actuator

---

## 3. 개발 단계

### 1단계: InfluxDB 설정 (Docker Compose)
**`docker-compose.yml`**
```yaml
version: "3.8"

services:
  mariadb:
    image: mariadb:10.9
    container_name: mariadb_for_spring
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: demo
      MYSQL_USER: demo
      MYSQL_PASSWORD: demo
    volumes:
      - mariadb_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  influxdb:
    image: influxdb:2.7
    container_name: influxdb_for_spring
    ports:
      - "8086:8086"
    volumes:
      - influxdb_data:/var/lib/influxdb2
    environment:
      - DOCKER_INFLUXDB_INIT_MODE=setup
      - DOCKER_INFLUXDB_INIT_USERNAME=my-user
      - DOCKER_INFLUXDB_INIT_PASSWORD=my-password
      - DOCKER_INFLUXDB_INIT_ORG=my-org
      - DOCKER_INFLUXDB_INIT_BUCKET=my-bucket
      - DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=my-super-secret-token
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8086/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  spring-app:
    build: .
    container_name: spring_app
    ports:
      - "8080:8080"
    depends_on:
      mariadb:
        condition: service_healthy
      influxdb:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mariadb://mariadb:3306/demo
      SPRING_DATASOURCE_USERNAME: demo
      SPRING_DATASOURCE_PASSWORD: demo
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      INFLUX_URL: http://influxdb:8086   # ✅ localhost 대신 서비스명
      INFLUX_TOKEN: my-super-secret-token
      INFLUX_ORG: my-org
      INFLUX_BUCKET: my-bucket

volumes:
  mariadb_data:
  influxdb_data:

```
실행:
```bash
docker-compose up -d --build
```

📌 설명
1. MariaDB: 애플리케이션의 RDBMS 저장소. Spring Data JPA와 연결해 메타데이터나 일반 데이터 관리
2. InfluxDB: 시계열 데이터베이스. 센서 데이터 같은 시계열 정보를 빠르게 저장·조회 가능
3. Spring App: Spring Boot 기반 애플리케이션. 위 두 DB와 연결되어 API 요청을 처리
4. healthcheck: MariaDB와 InfluxDB가 완전히 기동된 후 Spring App이 실행되도록 보장

---

### 2단계: Gradle 프로젝트 설정

**`build.gradle`**
```groovy
plugins {
    id "org.springframework.boot" version "3.3.4"   // ✅ Spring Boot 최신 버전
    id "io.spring.dependency-management" version "1.1.6"

    // Kotlin (Repository 구현체용)
    id "org.jetbrains.kotlin.jvm" version "1.9.25"
    id "org.jetbrains.kotlin.plugin.spring" version "1.9.25"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation "org.springframework.boot:spring-boot-starter-web"

    // ✅ InfluxDB Java Client
    implementation "com.influxdb:influxdb-client-java:6.10.0"

    // Kotlin
    implementation "org.jetbrains.kotlin:kotlin-reflect"
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8"

    // Lombok (Java DTO, Domain)
    compileOnly "org.projectlombok:lombok"
    annotationProcessor "org.projectlombok:lombok"
    testCompileOnly "org.projectlombok:lombok"
    testAnnotationProcessor "org.projectlombok:lombok"

    testImplementation "org.springframework.boot:spring-boot-starter-test"
}

tasks.named("test") {
    useJUnitPlatform()
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

```
📌 주요 변경점
1. Spring Boot `2.7.x` → `3.3.4`
2. Java 11 → **Java 17 (Toolchain 적용)**
3. Kotlin 플러그인 추가 (Repository를 Kotlin으로 작성 가능)
4. Lombok으로 Java 클래스 보일러플레이트 제거 (@Data, @Builder 등)
5. InfluxDB 클라이언트 추가 (influxdb-client-java)

---

### 3단계: 애플리케이션 설정

**`application.properties`**
```properties
# InfluxDB 연결 설정 (환경변수 우선)
influx.url=${INFLUX_URL:http://localhost:8086}
influx.token=${INFLUX_TOKEN:my-super-secret-token}
influx.org=${INFLUX_ORG:my-org}
influx.bucket=${INFLUX_BUCKET:my-bucket}
```

📌 설명
1. influx.url → InfluxDB 서버 주소 (로컬 실행 시 localhost, Docker Compose 실행 시 influxdb 서비스명 사용).
2. influx.token → 인증용 토큰 (관리자 계정 생성 시 발급된 값).
3. influx.org → InfluxDB 조직(Org) 이름.
4. influx.bucket → 시계열 데이터를 저장할 버킷 이름.

👉 모든 값은 환경 변수 우선 적용 후, 지정되지 않으면 application.properties의 기본값 사용

---

### 4단계: 프로젝트 구조
```
src/main/java/com/example/demo/
 ├── controller/
 │    └── ApiController.java
 ├── domain/
 │    └── SensorMeasurement.java
 ├── dto/
 │    ├── SensorMeasurementRequest.java
 │    └── SensorMeasurementResponse.java
 ├── service/
 │    └── ApiService.java
src/main/kotlin/com/example/demo/repository/
 └── SensorMeasurementRepository.kt
```
- **Java**: Entity, DTO, Controller, Service
- **Kotlin**: Repository 인터페이스

---

### 5단계: Dockerfile (멀티스테이지 빌드)

**`Dockerfile`**
```dockerfile
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
```

📌 설명
1. Builder Stage
    - gradle:8.10-jdk17-alpine 이미지를 사용해 빌드 환경을 구성.
    - build.gradle과 gradle.properties를 먼저 복사하여 의존성 캐싱 최적화.
    - gradle clean build -x test로 테스트를 제외한 최종 JAR 파일 생성.
2. Runtime Stage
    - 빌드 산출물(app.jar)만 가져와 경량 런타임 이미지(eclipse-temurin:17-jdk-alpine)에서 실행.
    - 결과적으로 빌드 도구나 캐시가 포함되지 않아 이미지 크기가 최소화됨.

👉 CI/CD 파이프라인 최적화와 운영 환경 경량화가 동시에 가능

---

### 6단계: 실행 및 확인
```bash
docker-compose up -d --build
```
- **InfluxDB UI** → http://localhost:8086  
- **Spring Boot API** → http://localhost:8080/api/measurements  

**[다시 빌드 & 실행]**
```bash
docker-compose down -v
docker-compose up -d --build
```

**[로그]**
```bash
docker logs -f spring_app
```
---

### 7단계: API 테스트

**[데이터 저장]**
```bash
curl -X POST http://localhost:8080/api/measurements      -H "Content-Type: application/json"      -d '{"sensorId": 1, "value": 23.5}'
```

**[데이터 조회]**
```bash
curl "http://localhost:8080/api/measurements/1?durationSec=600"
```

---

## 4. 검색 최적화 (Tag 유지 전략)
- InfluxDB에서 sensorId는 tag로 저장 → 고성능 필터링 가능
- InfluxDB는 tag를 반드시 문자열(String) 로 저장하므로 내부 저장은 문자열 기반으로 처리
- API 인터페이스는 여전히 Long 타입을 사용하여 개발자 경험을 유지
- 서비스 계층에서 요청/응답 시 String ↔ Long 변환을 수행하여 호환성 보장

📌 이 방식은 쿼리 성능을 최적화하면서도, 외부 API와 내부 데이터 모델의 불일치를 최소화할 수 있음

---
## 5. 빌드 & 설정 팁
`gradle.properties`
```
kapt.include.compile.classpath=false
org.gradle.jvmargs=-Xmx1024m -Djdk.compiler.disableAnnotationProcessing=false
```
- Kotlin + Java 혼합 환경에서 불필요한 클래스패스 포함 방지
- Lombok annotation processor가 반드시 동작하도록 보장
- 빌드 JVM 힙 메모리 제한(1GB)으로 안정성 확보

`lombok.config`
```
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
lombok.anyConstructor.addConstructorProperties = true
```
- 상위 디렉토리 설정 전파 방지 (stopBubbling)
- Lombok이 생성한 코드에 @Generated 추가 → IDE에서 자동 생성 코드 표시
- 모든 생성자에 @ConstructorProperties 적용 → 직렬화/역직렬화 및 JPA 호환성 강화

📌 이 설정들은 Kotlin + Java 혼합 프로젝트에서 Lombok 안정성 확보와 호환성 개선에 필수적

---

## 6. 확장 아이디어
- 기간별 조회 API (`start`, `end` 파라미터)
- 평균/최대/최소값 집계 API
- `@Scheduled` 더미 데이터 생성기
- Spring Boot Actuator + Grafana 대시보드
- CI/CD (GitHub Actions, Jenkins 등)
