package com.group.myos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MyOSApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyOSApplication.class, args);
    }

}
