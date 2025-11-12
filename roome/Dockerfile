 1. JDK 17이 포함된 경량 Linux 환경을 사용
FROM openjdk:17-jdk-slim

# 2. 작성자 정보
LABEL authors="yubanghyeon"

# 3. 작업 디렉토리 생성 (컨테이너 내부에서 실행할 폴더)
WORKDIR /roome

# 4. .env 파일 복사
COPY .env .env

# 5. JAR 파일을 컨테이너 내부로 복사
COPY build/libs/*.jar app.jar

# 6. Spring Boot 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]

### local 환경 ###

## 1. Java 17 OpenJDK 이미지 사용
#FROM openjdk:17-jdk
#
## 2. 앱 실행 폴더 생성
#WORKDIR /app
#
## 3. 빌드된 JAR 파일을 복사
#COPY build/libs/app.jar app.jar
#
## 4. 앱 실행
#ENTRYPOINT ["java", "-jar", "app.jar"]
