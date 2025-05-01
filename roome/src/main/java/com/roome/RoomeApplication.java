package com.roome;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableAsync
public class RoomeApplication {

  public static void main(String[] args) {
    // .env 파일 로드
    Dotenv dotenv = Dotenv.configure()
            .directory("./roome")
            .load();

    // 환경변수를 시스템 프로퍼티에 추가
    dotenv.entries().forEach(entry -> {
              System.setProperty(entry.getKey(), entry.getValue());
              System.out.println("🔐 ENV Loaded: " + entry.getKey() + " = " + entry.getValue());
            }
    );

    SpringApplication.run(RoomeApplication.class, args);
  }

}
