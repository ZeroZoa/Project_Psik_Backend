package com.zerozoa.skinner;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SkinnerApplication {

	public static void main(String[] args) {
        //.env 파일 로드 (없으면 무시하도록 ignoreIfMissing 추가 가능)
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        //로드한 환경변수를 Spring이 쓸 수 있게 시스템 프로퍼티로 설정
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        //스프링 실행
        SpringApplication.run(SkinnerApplication.class, args);
	}

}
