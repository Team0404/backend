package com.sparta.slack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.sparta")
public class SlackServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlackServiceApplication.class, args);
    }
}
