# 🚀 Spring Boot 기반 기상 데이터 수집·저장 시스템

## 1. 프로젝트 목표
- Spring Boot 3.x와 MariaDB, InfluxDB를 활용해 **기상청 OpenAPI 허브(KMA API)** 데이터를 자동 수집·저장·조회할 수 있는 시스템을 구현합니다.
- 단기예보 개황 등 텍스트 기반 데이터는 **MariaDB**에, 실시간 지상관측 시계열 데이터(기온, 풍속, 풍향, 기압, 강수량 등)는 **InfluxDB**에 저장합니다.
- Spring Boot 애플리케이션은 KMA API와 주기적으로 연동해 데이터를 적재하고, REST API를 통해 조회 기능을 제공합니다.
- Java + Kotlin 혼합 환경으로 Repository 계층 일부는 Kotlin으로 구현했습니다.
- Docker Compose를 활용하여 **MariaDB + InfluxDB + Spring App**을 일괄 실행할 수 있도록 구성했습니다.

---

## 2. 기술 스택
- **Backend:** Spring Boot 3.3.x, Java 17, Kotlin
- **Database:** MariaDB 10.9 (예보/메타데이터), InfluxDB 2.x (실시간 시계열 데이터)
- **Build Tool:** Gradle 8.10
- **Container:** Docker (멀티스테이지 빌드)
- **Data Source:** KMA 기상청 OpenAPI (지상관측)
- **Monitoring (선택):** InfluxDB UI, Grafana, Spring Boot Actuator

---

## 3. 시스템 아키텍처
```text
                   ┌────────────────────────┐
                   │       KMA API Hub       │
                   │ ─────────────────────── │
                   │ ① kma_sfctm3.php (실황) │
                   │ ② fct_afs_ds.php (예보) │
                   └─────────────┬──────────┘
                                 ↓
                  ┌────────────────────────┐
                  │   Spring Boot App       │
                  │ ─────────────────────── │
                  │ - RestTemplate 호출      │
                  │ - 응답 파싱              │
                  │ - 엔티티 매핑            │
                  │   (SensorMeasurement,    │
                  │    ForecastSummary)      │
                  └─────────┬───────┬───────┘
                            │       │
         ┌──────────────────┘       └──────────────────┐
         ↓                                           ↓
┌────────────────────────┐               ┌────────────────────────┐
│   InfluxDB (시계열 DB)   │               │   MariaDB (관계형 DB)   │
│ ─────────────────────── │               │ ─────────────────────── │
│ Measurement: sensor_data │               │ Table: forecast_summary │
│ Tags: sensor, station    │               │ PK: (tm_fc, stn_id)    │
│ Fields: value            │               │ 예보문/강수량/특보 저장 │
└─────────────┬──────────┘               └─────────────┬──────────┘
              ↓                                      ↓
┌────────────────────────┐               ┌────────────────────────┐
│  REST API (/measurements)│              │ REST API (/forecast)   │
│  - 실시간 시계열 조회     │              │ - stnId, 기간별 예보 조회│
└─────────────┬──────────┘               └─────────────┬──────────┘
              ↓                                      ↓
     ┌─────────────────────┐               ┌─────────────────────┐
     │ Grafana / Dashboard │               │ 외부 서비스 공유     │
     │ 시각화/모니터링       │               │ 대시보드/분석/연계   │
     └─────────────────────┘               └─────────────────────┘

```
- 실황 (kma_sfctm3 → InfluxDB → Measurements API → Grafana)
- 예보 (fct_afs_ds → MariaDB → Forecast API → 외부 서비스 공유)
---

## 4. 개발 단계

### 1단계: Docker Compose 설정 
**`docker-compose.yml`**
```yaml
version: "3.8"

services:
  mariadb:
    image: mariadb:10.9
    container_name: mariadb_for_spring
    ports:
      - "3307:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: ${TZ}
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
      - DOCKER_INFLUXDB_INIT_USERNAME=${INFLUXDB_USERNAME}
      - DOCKER_INFLUXDB_INIT_PASSWORD=${INFLUXDB_PASSWORD}
      - DOCKER_INFLUXDB_INIT_ORG=${INFLUXDB_ORG}
      - DOCKER_INFLUXDB_INIT_BUCKET=${INFLUXDB_BUCKET}
      - DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=${INFLUXDB_TOKEN}
      - TZ=${TZ}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8086/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  spring-app:
    image: springboot_kma
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
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: ${SPRING_JPA_HIBERNATE_DDL_AUTO}
      INFLUX_URL: http://influxdb:8086
      INFLUX_TOKEN: ${INFLUXDB_TOKEN}
      INFLUX_ORG: ${INFLUXDB_ORG}
      INFLUX_BUCKET: ${INFLUXDB_BUCKET}
      KMA_AUTH_KEY: ${KMA_AUTH_KEY}   # ✅ 기상청 API KEY 전달
      TZ: ${TZ}

volumes:
  mariadb_data:
  influxdb_data:

```
📌 설명
1. MariaDB: 애플리케이션의 RDBMS 저장소. Spring Data JPA와 연결해 메타데이터나 일반 데이터 관리
2. InfluxDB: 시계열 데이터베이스. 센서 데이터 같은 시계열 정보를 빠르게 저장·조회 가능
3. Spring App: Spring Boot 기반 애플리케이션. 위 두 DB와 연결되어 API 요청을 처리
4. healthcheck: MariaDB와 InfluxDB가 완전히 기동된 후 Spring App이 실행되도록 보장
5. TZ: 애플리케이션 서버의 시간대 지정 (KST 기준 동작 보장)
6. 환경변수: .env 파일로 관리

**실행 방법**
```
docker-compose --env-file .env up -d --build
```
**컨테이너 상태 확인**
```
docker ps
docker logs -f spring_app
docker logs -f influxdb_for_spring
docker logs -f mariadb_for_spring
```
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
    implementation "org.springframework.boot:spring-boot-starter-data-jpa"   // ✅ JPA + jakarta.persistence
    implementation "org.springframework.boot:spring-boot-starter-webflux"    // ✅ WebClient
    implementation "org.mariadb.jdbc:mariadb-java-client"                    // ✅ MariaDB driver

    // InfluxDB Client
    implementation "com.influxdb:influxdb-client-java:6.10.0"

    // Kotlin
    implementation "org.jetbrains.kotlin:kotlin-reflect"
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8"

    // Lombok
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
6. Spring Data JPA 추가 (spring-boot-starter-data-jpa) → jakarta.persistence 기반 엔티티/레포지토리 지원
7. Spring WebFlux 추가 (spring-boot-starter-webflux) → WebClient 활용, 기상청 API 비동기 호출 가능
8. MariaDB JDBC 드라이버 추가 (mariadb-java-client) → MariaDB와 안정적으로 연동

---

### 3단계: 애플리케이션 설정

**`application.properties`**
```properties
# InfluxDB 연결
influx.url=${INFLUX_URL:http://localhost:8086}
influx.token=${INFLUX_TOKEN:my-super-secret-token}
influx.org=${INFLUX_ORG:my-org}
influx.bucket=${INFLUX_BUCKET:demo_bucket}

# KMA API
kma.base-url=https://apihub.kma.go.kr/api/typ01/url/kma_sfctm3.php
kma.fct-url=https://apihub.kma.go.kr/api/typ01/url/fct_afs_ds.php
kma.auth-key=${KMA_AUTH_KEY}
kma.station=108

```

📌 설명
1. `influx.url` : InfluxDB 서버 주소
    - 로컬 실행 시: http://localhost:8086
    - Docker Compose 실행 시: http://influxdb:8086 (서비스명 사용)
2. `influx.token` : InfluxDB 접속을 위한 인증 토큰
    - 초기 설정 시 생성되는 Admin Token 사용
    - 보안상 .env 또는 환경 변수로 관리하는 것이 권장됨
3. `influx.org` : InfluxDB 내 조직(Organization) 이름
    - 버킷(bucket)과 함께 데이터 저장/조회 시 필요
4. `influx.bucket` : 시계열 데이터를 저장할 버킷 이름
    - 데이터베이스(Database)와 유사한 개념
5. `kma.base-url` : 기상청 실시간 날씨 API URL (시간 단위 지상관측 데이터 제공)
    - 현재 사용: kma_sfctm3.php 
6. `kma.fct-url` : 기상청 단기예보 API URL
    - 현재 사용: fct_afs_ds.php
7. `kma.auth-key` : 기상청 OpenAPI 인증 키(API Key)
    - 데이터포털에서 발급받아야 하며, 필수적으로 쿼리 파라미터에 포함되어야 함
    - env로 관리
8. `kma.station` : 관측소 지점 번호(STN 코드)
    - 108은 서울 관측소
    - 다른 지역의 코드를 넣으면 해당 지점의 데이터 수집 가능
    - 119 수원 / 112 인천 / 143 강릉 / 156 대전
    - 159 부산 / 189 제주 / 185 여수 / 146 울릉도

🔹 동작 원리
- Spring Boot는 환경 변수 > properties 파일 순서로 값을 읽음
- 따라서 docker-compose.yml에서 .env 파일을 연결하면 환경 변수가 우선 적용됨
- 별도 환경 변수를 지정하지 않으면 application.properties의 기본값이 적용됨

👉 모든 값은 환경 변수 우선 적용 후, 지정되지 않으면 application.properties의 기본값 사용

---

### 4단계: 프로젝트 구조
```
src/main/java/com/example/demo/
 ├── config/
 │    └── InfluxDBConfig.java               # InfluxDB 연결 설정
 ├── constants/
 │    └── HttpStatusCodeContrants.java      # 상태 코드
 ├── util/                                  # 유틸리티 (시간, 공통 함수)
 │    └── TimeUtils.java
 ├── controller/                            # REST API 엔드포인트
 │    ├── ForecastSummaryController.java
 │    ├── KmaController.java                # KMA 데이터 수집 API
 │    └── MeasurementController.java        # 센서 데이터 저장/조회 API
 ├── domain/                                # 도메인 엔티티 (관측 데이터)
 │    ├── ForecastSummary.java
 │    ├── Sensor.java                       # 센서 엔티티
 │    └── SensorMeasurement.java            # 센서 측정값 엔티티
 ├── dto/                                   # 데이터 전송 객체
 │    ├── AdminResponse.java                # 공통 응답 Wrapper
 │    ├── SensorMeasurementRequest.java     # 요청 DTO (센서ID, 값)
 │    └── SensorMeasurementResponse.java    # 응답 DTO (센서ID, 값, 시각)
 ├── fixture/
 │    └── ServerInitializationFixture.java  # 서버 실행 시 기본 데이터 삽입
 ├── repository/                            # Repository 인터페이스
 │    ├── SensorRepository.java             # JPA 기반 Sensor Repository
 │    └── InfluxDBRepository.java           # InfluxDB 저장/조회 인터페이스
 ├── service/
 │    ├── ForecastSummaryService.java
 │    ├── KmaService.java                   # KMA API 호출 + InfluxDB 적재
 │    └── MeasurementService.java           # 센서 데이터 저장/조회 서비스
 ├── DemoApplication.java                   # Spring Boot 실행 클래스

src/main/kotlin/com/example/demo/repository/
 ├── ForecastSummaryRepository.kt
 └── InfluxDBRepositoryImpl.kt              # InfluxDBRepository Kotlin 구현체

resources/
 └── application.properties                 # 환경설정 파일

```

- **Java**: Controller, Service, Entity, DTO 등 핵심 비즈니스 로직
- **Kotlin**: InfluxDBRepositoryImpl 구현 → Java + Kotlin 혼합 환경 실험
- **AdminResponse**: 모든 API 응답을 status + payload 형식으로 통일
- **fixture**: 서버 구동 시 기본 데이터 세팅

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
docker-compose --env-file .env up -d --build
```
- **InfluxDB UI** → http://localhost:8086  
- **Spring Boot API** → http://localhost:8080/api/measurements  

**[다시 빌드 & 실행]**
```bash
docker-compose down -v
docker-compose --env-file .env up -d --build
```

**[로그]**
```bash
docker logs -f spring_app
```
---

### 7단계: API 테스트

**1. 데이터 저장 (수동 저장 API)**
```bash
curl -X POST http://localhost:8080/api/measurements \
  -H "Content-Type: application/json" \
  -d '{"sensorId": 1, "value": 23.5}'
```

**2. 데이터 조회 (sensorId 기반)**
```bash
curl "http://localhost:8080/api/measurements/1?durationSec=3600"
```

**3. 데이터 조회 (sensorName 기반)**
```bash
curl "http://localhost:8080/api/measurements/by-name/temperature?durationSec=3600"
```


**4. KMA 데이터 수집 (기상청 API → InfluxDB 적재)**
```bash
curl -X POST "http://localhost:8080/api/kma/fetch?tm1=2025090100&tm2=2025090200"
```
  📌 파라미터 설명
   - tm1, tm2: 조회 기간 (시작/종료 시각)
   -  형식: yyyyMMddHH   
        예) 2025090100 → 2025년 9월 1일 00시   
        예) 2025090200 → 2025년 9월 2일 00시

**5. 전체 데이터 조회 (⚠️ 성능 주의, 개발용)**
```bash
curl "http://localhost:8080/api/measurements/all"
```

**6. 기간 지정 데이터 조회**
```bash
curl "http://localhost:8080/api/measurements/list?sensorName=temperature&start=2025-09-01T00:00:00&end=2025-09-08T23:59:59"
```

**7. 기간 지정 데이터 조회 (센서별 그룹핑)**
```bash
curl "http://localhost:8080/api/measurements/list/grouped?start=2025-09-01T00:00:00&end=2025-09-08T23:59:59"
```

**7. 단기 예보 데이터 수집**
```bash
curl -X POST "http://localhost:8080/api/forecast?tmfc1=2025091106&tmfc2=2025091118"
```


### 포스트맨
[SpringBoot_KMA](https://documenter.getpostman.com/view/20595515/2sB3Hks21J)

---

## 5. 데이터 처리 흐름
**(1) 실시간 관측 데이터 (KMA → InfluxDB)**
  1. 데이터 수집
    - `@Scheduled` 스케줄러가 매 정시마다 기상청 API 호출
    - 관측 지점(station=108)의 실시간 기상 데이터를 텍스트 포맷으로 수신
  2. 데이터 파싱
    - 응답 라인 단위 파싱
    - 주요 관측값 추출:
      - `TA` (기온, ℃)
      - `WS` (풍속, m/s)
      - `WD` (풍향, 16방위)
      - `PA` (현지기압, hPa)
      - `RN` (강수량, mm)
  3. InfluxDB 적재
    - Measurement: `sensor_data`
    - Tags: sensor, `station`
    - Field: `value`
    - Time: 관측 시각(TM → UTC 변환)
  4. 데이터 조회
    - Spring Boot REST API 엔드포인트 제공
    - 특정 센서(`temperature`, `wind_speed`, `pressure` 등)에 대해 기간별 시계열 조회 가능

**(2) 단기예보 개황 데이터 (KMA → MariaDB)**
  1. 데이터 수집
    - `@Scheduled` 스케줄러가 매 6시간마다 기상청 단기예보 개황 API 호출
    - 예보관(stnId=108)의 **예보 요약 데이터**를 JSON 포맷으로 수신
  2. 데이터 파싱
    - JSON 응답에서 주요 필드 추출:
      - `tm_fc` (발표 시각)
      - `man_fc` (예보관명)
      - `wf_sv1/2/3` (기상 개황: 오늘/내일/모레)
      - `wn` (특보사항), `wr` (예비특보), `rem` (비고)
  3. MariaDB 적재
    - Entity: `ForecastSummary`
    - Unique Key: `(tm_fc, stn_id)`
    - Upsert 처리 (`ON DUPLICATE KEY UPDATE`) → 중복 데이터 방지
  4. 데이터 조회
    - REST API로 특정 관서 + 시간 범위 조건 검색 가능
    - 예: `stnId=108, tmFc BETWEEN 2025-09-11 00:00 ~ 2025-09-12 00:00`
---

## 6. 최적화
**(1) 실시간 관측 데이터 (InfluxDB)**
  - `sensorId`는 `tag`로 저장 → 고성능 필터링 가능
  - InfluxDB는 tag를 반드시 문자열(String) 로 저장하므로 내부 저장은 문자열 기반으로 처리
  - API 인터페이스는 여전히 **Long 타입**을 사용하여 개발자 경험(타입 안정성) 유지
  - 서비스 계층에서 요청/응답 시 `String ↔ Long` 변환을 수행하여 호환성 보장

📌 이 방식은 쿼리 성능 최적화 + 개발자 경험 유지라는 두 가지 목표를 동시에 달성

**(2) 단기예보 개황 데이터 (MariaDB)**
  - `ForecastSummary` 테이블은 `(tm_fc, stn_id)`에 **Unique Index** 적용
  - 중복 데이터 발생 시 ON DUPLICATE KEY UPDATE 방식으로 **Upsert** 처리
  - 불필요한 insert 방지 → 무결성 보장
  - 대용량 데이터 증가 시에도 시간 기반 파티셔닝(tm_fc) 고려 가능

📌 이를 통해 데이터 중복 방지 + 최신 상태 유지를 동시에 보장

---
## 7. 빌드 & 설정 팁
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
## 8. 추가 작업 내역
### ✅ MariaDB 접속 및 확인
- 컨테이너 실행 후 MariaDB에 직접 접속해 테이블 생성 여부 및 초기 Sensor 데이터 확인:
```
docker exec -it mariadb_for_spring sh
mariadb -u demo -p
```
- 테이블 조회:
```
SHOW DATABASES;
USE demo;

SHOW TABLES;
SELECT * FROM sensors;
```
👉 ServerInitializationFixture 에 의해 서버 기동 시 Sensor 엔티티(온도, 풍속, 풍향 등)가 자동 등록되는지 확인할 수 있음.

### ✅ InfluxDB 데이터 확인
- InfluxDB는 UTC 기준으로 저장됨.
- Data Explorer 기본 범위(Past 1h)에서 결과가 안 보이면 Past 12h 또는 Past 24h로 확장해야 함.
- Flux Script 직접 실행:
  ```
  from(bucket: "demo_bucket")
    |> range(start: -24h)
    |> filter(fn: (r) => r._measurement == "sensor_data")
  ```
  👉 여기서 sensor=temperature, station=108 등의 태그로 필터링 가능.
- 📌 왜 from(bucket: "...")을 꼭 써야 하나?
  - InfluxDBClientFactory.create(url, token, org, bucket)에서 설정한 bucket은 쓰기(Write API) 기본값으로만 사용됨.
  ```
  writeApi.writeMeasurement(WritePrecision.MS, measurement);
  // 여기서는 Config에 지정한 bucket/org 자동 적용
  ```
  - 조회(Query API)는 Flux 스크립트를 서버로 그대로 보내기 때문에, 어떤 버킷에서 데이터를 읽을지는 Flux에서 직접 명시해야 함.
  ```
  from(bucket: "demo_bucket")  // ✅ 반드시 지정 필요
  ```
  - Write → Config 기본값 자동 적용
  - Query → Flux 구문에서 명시적으로 bucket 지정해야 함

- 📌 컨테이너 안에서 리눅스 쉘 사용 (추천)
  ```
  docker exec -it influxdb_for_spring sh
  ```
  - 위 명령으로 들어가면 # 프롬프트가 뜸 → 여기서 Influx CLI 실행 가능
  ```
  influx bucket list --org demo_org --token my-super-secret-token

  influx query '
  from(bucket: "demo_bucket")
    |> range(start: 0)
    |> filter(fn: (r) => r._measurement == "sensor_data")
    |> filter(fn: (r) => r["sensor"] == "wind_speed")
    |> filter(fn: (r) => r._field == "value")
    |> timeShift(duration: 9h)  // ✅ UTC → KST (9시간 더하기)
  ' --org demo_org --token my-super-secret-token
  ```
  👉 PowerShell에서는 따옴표 처리 때문에 명령어가 깨질 수 있으므로, 컨테이너 안 리눅스 쉘에서 실행하는 걸 추천.

### ✅ 태그 키 일관성 문제
- KmaService → 태그 이름 sensor
- MeasurementService + SensorMeasurement → 태그 이름 sensorId

👉 Flux 쿼리에서 불일치 발생 → 조회 결과가 비어 있음.   
해결책: SensorMeasurement 클래스에서 태그 이름을 강제로 맞춤.
```
@Measurement(name = "sensor_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorMeasurement {

    @Column(tag = true, name = "sensor")   // ✅ 태그 이름 통일
    private String sensorId;

    @Column
    private Double value;

    @Column(name = "_time", timestamp = true) // ✅ 실제 데이터 시각
    private Instant sensingDate;
}
```
👉 이렇게 하면 KmaService와 MeasurementService 모두 sensor 태그를 사용하므로 조회/저장이 일관성 있게 작동함.

### ✅ 서버 기동 시 하루치 데이터 초기 적재
- 기본 센서 등록과 동시에, 서버가 시작될 때 `kma.init-days` 설정값(기본 31일) 전 00시 ~ 현재 정시까지 데이터를 한 번 수집 및 저장.
- 구현: `ServerInitializationFixture` 내부에서 `fetchAndStoreInitialData()` 메서드 추가.
- 중복 방지 플래그
    - `KmaService`의 `@PostConstruct init()`에서 `initialized = true`로 세팅
    - 첫 번째 스케줄 실행 시 `initialized == true`이면 스킵 후 `false`로 변경 → 초기 적재와 스케줄 적재가 겹쳐 중복 저장되는 문제 방지
- 동작
  1. MariaDB에 Sensor 엔티티가 존재하지 않으면 기본값 등록
  2. KMA API 호출: `init-days` 전 00시 ~ 현재 정시까지의 데이터를 InfluxDB에 적재
  3. 스케줄러 실행: `KmaService`의 `@Scheduled(cron = "0 10 * * * *")`가 매 시각 10분마다 최신 1시간 데이터를 적재

👉 이 방식으로 서버 재기동 후에도 과거 ~ 현재까지의 데이터가 보존되며, 스케줄러가 이어받아 최신 데이터 적재를 지속적으로 보장 및 서버 재기동 후에도 데이터 누락 없음 + 중복 적재 방지 두 가지가 모두 보장

---

## 9. 확장 아이디어
- 평균/최대/최소값 집계 API
- Spring Boot Actuator + Grafana 대시보드
- CI/CD (GitHub Actions, Jenkins 등)
- 단기예보
  - 기상청 단기예보 개황 API → Spring Boot → MariaDB 저장 → REST API로 공유