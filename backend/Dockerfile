FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
# Gradle 보관 폴더(/root/.gradle)를 빌드 사이에 재사용한다. (이미지에는 들어가지 않는다)
# 코드가 바뀌어 이 줄이 다시 실행돼도, 이미 받은 Gradle과 라이브러리는 다시 받지 않고 컴파일만 한다.
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/build/libs/remake.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
