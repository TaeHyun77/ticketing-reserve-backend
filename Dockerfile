# ===== 빌드 스테이지 =====
FROM amazoncorretto:17 AS builder

# gradlew 가 클래스패스 구성에 xargs 를 쓰는데 corretto 베이스에 findutils 가 없어 빌드가 실패한다 → 설치.
RUN yum install -y findutils && yum clean all

WORKDIR /app

# Gradle Wrapper + 설정 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# 의존성 먼저 다운로드 (소스 변경 시 이 레이어는 캐시됨)
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon

# ===== 실행 스테이지 =====
FROM amazoncorretto:17-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
