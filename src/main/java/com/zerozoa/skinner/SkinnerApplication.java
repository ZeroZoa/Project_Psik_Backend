package com.zerozoa.skinner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SkinnerApplication {

	public static void main(String[] args) {
        //스프링 실행
        SpringApplication.run(SkinnerApplication.class, args);
	}

}
