package com.wayflo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class WayFloApplication {

    public static void main(String[] args) {
        SpringApplication.run(WayFloApplication.class, args);
    }
}
