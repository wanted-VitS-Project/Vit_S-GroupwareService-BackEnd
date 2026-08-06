package com.group3.vitamins;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VitaminSApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaminSApplication.class, args);
    }

}
