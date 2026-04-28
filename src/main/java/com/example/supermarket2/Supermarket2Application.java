package com.example.supermarket2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 开启定时任务
public class Supermarket2Application {

    public static void main(String[] args) {
        SpringApplication.run(Supermarket2Application.class, args);
    }

}
