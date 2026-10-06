package com.maison.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MaisonBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaisonBackendApplication.class, args);
    }
}
