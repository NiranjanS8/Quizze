package com.quizze.quizze;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class QuizzeApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizzeApplication.class, args);
    }
}
