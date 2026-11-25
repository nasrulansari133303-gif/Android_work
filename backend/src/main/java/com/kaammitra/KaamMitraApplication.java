package com.kaammitra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KaamMitraApplication {
    public static void main(String[] args) {
        SpringApplication.run(KaamMitraApplication.class, args);
    }
}
