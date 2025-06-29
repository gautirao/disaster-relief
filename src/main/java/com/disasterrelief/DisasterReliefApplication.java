package com.disasterrelief;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.disasterrelief")
public class DisasterReliefApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisasterReliefApplication.class, args);
    }
}
