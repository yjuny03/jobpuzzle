package com.example.jobpuzzle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class JobpuzzleApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobpuzzleApplication.class, args);
    }

}
