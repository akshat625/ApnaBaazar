package com.apnabaazar.apnabaazar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableAsync
public class ApnaBaazarApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApnaBaazarApplication.class, args);
    }

}
